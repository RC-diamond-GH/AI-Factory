package com.rcdiamond.aifactory.network

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.client.llm.ExecuteCommandRequest
import com.rcdiamond.aifactory.client.llm.ExecuteCommandResult
import com.rcdiamond.aifactory.client.llm.GiveItemRequest
import com.rcdiamond.aifactory.client.llm.GiveItemResult
import com.rcdiamond.aifactory.server.CommandLimits
import com.rcdiamond.aifactory.server.ExecuteCommandMechanism
import com.rcdiamond.aifactory.server.GiveItemMechanism
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

/**
 * Server-authoritative game actions and their answers.
 *
 * Payloads live in the provider-independent llm package and contain no credentials or client-only
 * classes. Give-item requests use a fixed operation; execute-command requests retain the sender's
 * command permissions and context. The server never touches the tool registry, agent or LLM.
 *
 * ## Compatibility
 * The channel advertises exactly [PROTOCOL_VERSION] and both sides accept nothing but that same string.
 * A peer that does not have this channel (a vanilla server, or a client without the mod) is therefore
 * rejected during the Forge handshake instead of silently dropping messages, and an established connection
 * means both sides speak this version. [isAvailable] is still the runtime check to use, since it also
 * reports the negotiation result of a concrete connection.
 *
 * ## Registration
 * [register] must run once on every side during common setup, i.e. before Forge locks network registration
 * (`NETWORK_LOCK`). It performs no registry access, so calling it from `FMLCommonSetupEvent.enqueueWork` is
 * safe; repeat calls are ignored. Registration is common-safe: no client class is referenced here, and the
 * response events are only ever posted by the client-side handlers.
 */
object GameActionNetwork {
    /** Exact-match protocol version. Changing the wire format means changing this string. */
    const val PROTOCOL_VERSION: String = "2"

    private const val REQUEST_MESSAGE_ID = 0
    private const val RESULT_MESSAGE_ID = 1
    private const val COMMAND_REQUEST_MESSAGE_ID = 2
    private const val COMMAND_RESULT_MESSAGE_ID = 3

    /** Bound of the encoded item id; a longer string is a protocol violation and aborts the connection. */
    private const val MAX_ITEM_ID_LENGTH = 128

    /** Bound of the encoded result text; the mechanism stays far below it. */
    private const val MAX_RESULT_MESSAGE_LENGTH = 256

    private val LOGGER = LogUtils.getLogger()
    private val CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(AIFactory.MODID, "game_action")

    @Volatile
    private var channel: SimpleChannel? = null

    /**
     * Creates the channel and registers the messages. Call once from common setup before the network
     * registration is locked; later calls do nothing.
     */
    fun register() {
        if (channel != null) {
            LOGGER.debug("Game action channel is already registered")
            return
        }
        val created = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            { PROTOCOL_VERSION },
            { version -> version == PROTOCOL_VERSION },
            { version -> version == PROTOCOL_VERSION },
        )
        created.messageBuilder(GiveItemRequest::class.java, REQUEST_MESSAGE_ID, NetworkDirection.PLAY_TO_SERVER)
            .encoder { request, buffer -> encodeRequest(request, buffer) }
            .decoder { buffer -> decodeRequest(buffer) }
            .consumerMainThread { request, context -> handleRequest(request, context.get()) }
            .add()
        created.messageBuilder(GiveItemResult::class.java, RESULT_MESSAGE_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder { result, buffer -> encodeResult(result, buffer) }
            .decoder { buffer -> decodeResult(buffer) }
            .consumerMainThread { result, _ -> MinecraftForge.EVENT_BUS.post(GiveItemResponseEvent(result)) }
            .add()
        created.messageBuilder(ExecuteCommandRequest::class.java, COMMAND_REQUEST_MESSAGE_ID, NetworkDirection.PLAY_TO_SERVER)
            .encoder { request, buffer ->
                buffer.writeUUID(request.requestId)
                buffer.writeUtf(request.command, CommandLimits.MAX_COMMAND_LENGTH)
            }
            .decoder { buffer ->
                ExecuteCommandRequest(buffer.readUUID(), buffer.readUtf(CommandLimits.MAX_COMMAND_LENGTH))
            }
            .consumerMainThread { request, context -> handleCommandRequest(request, context.get()) }
            .add()
        created.messageBuilder(ExecuteCommandResult::class.java, COMMAND_RESULT_MESSAGE_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder { result, buffer ->
                buffer.writeUUID(result.requestId)
                buffer.writeBoolean(result.success)
                buffer.writeVarInt(result.resultCode)
                buffer.writeUtf(result.message, CommandLimits.MAX_OUTPUT_LENGTH)
                buffer.writeBoolean(result.outputTruncated)
            }
            .decoder { buffer ->
                ExecuteCommandResult(
                    buffer.readUUID(), buffer.readBoolean(), buffer.readVarInt(),
                    buffer.readUtf(CommandLimits.MAX_OUTPUT_LENGTH), buffer.readBoolean(),
                )
            }
            .consumerMainThread { result, _ -> MinecraftForge.EVENT_BUS.post(ExecuteCommandResponseEvent(result)) }
            .add()
        channel = created
    }

    /**
     * True when the remote end of [connection] has negotiated this channel, i.e. when a game-action request
     * may actually be sent. Common-safe: the caller supplies the connection, so no client type is needed
     * here. False before [register] ran, and false for a connection to a peer without the channel.
     */
    fun isAvailable(connection: Connection): Boolean = channel?.isRemotePresent(connection) == true

    /**
     * Sends one give-item request to the server. Client-only call: the request goes out over the client's
     * own connection (`PacketDistributor.SERVER`, the common-safe form of `SimpleChannel.sendToServer`) and
     * the answer arrives as a [GiveItemResponseEvent] on the client main thread, to be matched by
     * [GiveItemRequest.requestId].
     *
     * @throws IllegalStateException when called before [register] ran; guard with [isAvailable] first.
     */
    fun sendGiveItem(request: GiveItemRequest) {
        val instance = channel ?: error("GameActionNetwork.register() has not been called")
        instance.send(PacketDistributor.SERVER.noArg(), request)
    }

    fun sendExecuteCommand(request: ExecuteCommandRequest) {
        val instance = channel ?: error("GameActionNetwork.register() has not been called")
        instance.send(PacketDistributor.SERVER.noArg(), request)
    }

    private fun handleCommandRequest(request: ExecuteCommandRequest, context: NetworkEvent.Context) {
        val instance = channel ?: return
        val player = context.sender
        if (player == null) {
            LOGGER.warn("Execute-command request {} arrived without a player sender", request.requestId)
            instance.reply(ExecuteCommandResult(request.requestId, false, 0, "No player is attached to this request"), context)
            return
        }
        instance.reply(ExecuteCommandMechanism.execute(player, request), context)
    }

    /** Runs on the server main thread: validate, deliver, answer the sender, and nothing else. */
    private fun handleRequest(request: GiveItemRequest, context: NetworkEvent.Context) {
        val instance = channel ?: return
        val player = context.sender
        if (player == null) {
            LOGGER.warn("Give-item request {} arrived without a player sender", request.requestId)
            instance.reply(GiveItemResult(request.requestId, false, "No player is attached to this request"), context)
            return
        }
        // The reply goes back over the connection that sent the request, so it can not reach another player.
        instance.reply(GiveItemMechanism.grant(player, request), context)
    }

    private fun encodeRequest(request: GiveItemRequest, buffer: FriendlyByteBuf) {
        buffer.writeUUID(request.requestId)
        buffer.writeUtf(request.itemId, MAX_ITEM_ID_LENGTH)
        buffer.writeVarInt(request.count)
    }

    private fun decodeRequest(buffer: FriendlyByteBuf): GiveItemRequest {
        val requestId = buffer.readUUID()
        val itemId = buffer.readUtf(MAX_ITEM_ID_LENGTH)
        val count = buffer.readVarInt()
        return GiveItemRequest(requestId, itemId, count)
    }

    private fun encodeResult(result: GiveItemResult, buffer: FriendlyByteBuf) {
        buffer.writeUUID(result.requestId)
        buffer.writeBoolean(result.success)
        buffer.writeUtf(result.message, MAX_RESULT_MESSAGE_LENGTH)
    }

    private fun decodeResult(buffer: FriendlyByteBuf): GiveItemResult {
        val requestId = buffer.readUUID()
        val success = buffer.readBoolean()
        val message = buffer.readUtf(MAX_RESULT_MESSAGE_LENGTH)
        return GiveItemResult(requestId, success, message)
    }
}
