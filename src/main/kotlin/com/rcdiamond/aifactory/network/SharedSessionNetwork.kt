package com.rcdiamond.aifactory.network

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.shared.SharedSessionMechanism
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

/**
 * Transport for the shared-session RPC: bounded, chunked, JSON-only, and common-safe.
 *
 * Bodies travel as UTF-8 JSON split into chunks of at most [MAX_CHUNK_BYTES] because a Minecraft packet
 * cannot carry a full conversation; the two directions are fixed (requests only go to the server, replies
 * and terminal-open only go to the client), both assemble on the main thread, and an assembly is bounded by
 * [MAX_ASSEMBLED_BYTES] — tens of megabytes, enough for a full 256Ki cell's context, but not unbounded.
 *
 * No client-only class is referenced here, so registration and the client-side `request`/`openTerminal`
 * entry points can live in the common source set. The reply to a request is posted as a
 * [SharedSessionResponseEvent] the caller matches by request id.
 *
 * ## Compatibility
 * The channel advertises exactly [PROTOCOL_VERSION] and accepts nothing else on either side, the same
 * handshake policy as the game-action channel: a peer without the channel is rejected at the Forge
 * handshake rather than silently dropping messages.
 */
object SharedSessionNetwork {
    /** Exact-match protocol version. Changing the wire format means changing this string. */
    const val PROTOCOL_VERSION: String = "3"

    /** Largest payload carried by one chunk. */
    const val MAX_CHUNK_BYTES: Int = 24 * 1024

    /** Largest reassembled request or reply. Tens of megabytes covers a full 256Ki cell's context. */
    const val MAX_ASSEMBLED_BYTES: Long = 64L * 1024 * 1024

    /** Longest accepted terminal locator text. */
    const val MAX_LOCATOR_LENGTH: Int = 512

    /** Most assemblies one connection may have in flight at once. */
    private const val MAX_PENDING_ASSEMBLIES: Int = 16

    /** Largest chunk count that can still fit inside [MAX_ASSEMBLED_BYTES]. */
    private val MAX_CHUNKS: Int = ((MAX_ASSEMBLED_BYTES + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES).toInt()

    private const val REQUEST_MESSAGE_ID = 0
    private const val RESPONSE_MESSAGE_ID = 1
    private const val OPEN_MESSAGE_ID = 2

    private val LOGGER = LogUtils.getLogger()
    private val CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(AIFactory.MODID, "shared_session")

    @Volatile
    private var channel: SimpleChannel? = null

    /** Requests being assembled on the server, per player and then per request id. */
    private val serverAssemblies = HashMap<UUID, HashMap<UUID, ChunkAssembly>>()

    /** Replies being assembled on the client, per request id. */
    private val clientAssemblies = HashMap<UUID, ChunkAssembly>()

    /**
     * Creates the channel, registers the messages and installs the shared-session lease cleanup. Call once
     * from common setup before the network registration is locked; later calls do nothing.
     */
    fun register() {
        if (channel != null) {
            LOGGER.debug("Shared-session channel is already registered")
            return
        }
        val created = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            { PROTOCOL_VERSION },
            { version -> version == PROTOCOL_VERSION },
            { version -> version == PROTOCOL_VERSION },
        )
        created.messageBuilder(SharedSessionRequestMessage::class.java, REQUEST_MESSAGE_ID, NetworkDirection.PLAY_TO_SERVER)
            .encoder { message, buffer ->
                buffer.writeChunk(message.requestId, message.chunkIndex, message.chunkCount, message.data)
            }
            .decoder { buffer ->
                val chunk = buffer.readChunk()
                SharedSessionRequestMessage(chunk.requestId, chunk.index, chunk.count, chunk.data)
            }
            .consumerMainThread { message, context -> handleRequest(message, context.get()) }
            .add()
        created.messageBuilder(SharedSessionResponseMessage::class.java, RESPONSE_MESSAGE_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder { message, buffer ->
                buffer.writeChunk(message.requestId, message.chunkIndex, message.chunkCount, message.data)
            }
            .decoder { buffer ->
                val chunk = buffer.readChunk()
                SharedSessionResponseMessage(chunk.requestId, chunk.index, chunk.count, chunk.data)
            }
            .consumerMainThread { message, _ -> handleResponse(message) }
            .add()
        created.messageBuilder(SharedTerminalOpenMessage::class.java, OPEN_MESSAGE_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder { message, buffer -> buffer.writeUtf(message.locator, MAX_LOCATOR_LENGTH) }
            .decoder { buffer -> SharedTerminalOpenMessage(buffer.readUtf(MAX_LOCATOR_LENGTH)) }
            .consumerMainThread { message, _ -> handleOpen(message) }
            .add()
        channel = created
        SharedSessionMechanism.install()
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> {
            serverAssemblies.remove(it.entity.uuid)
        }
        MinecraftForge.EVENT_BUS.addListener<ServerStoppedEvent> { serverAssemblies.clear() }
    }

    /**
     * True when the remote end of [connection] has negotiated this channel. Common-safe: the caller supplies
     * the connection, so no client type is needed here. False before [register] ran.
     */
    fun isAvailable(connection: Connection): Boolean = channel?.isRemotePresent(connection) == true

    fun clearClientAssemblies() {
        clientAssemblies.clear()
    }

    /**
     * Sends one shared-session request to the server. Client-only call: the request goes out over the
     * client's own connection, and the answer arrives as a [SharedSessionResponseEvent] matched by
     * [requestId].
     *
     * @throws IllegalStateException when called before [register] ran; guard with [isAvailable] first.
     */
    fun request(requestId: UUID, operation: String, payload: JsonObject) {
        val instance = channel ?: error("SharedSessionNetwork.register() has not been called")
        val body = buildJsonObject {
            put("operation", operation)
            put("payload", payload)
        }.toString().toByteArray(Charsets.UTF_8)
        sendChunks(instance, PacketDistributor.SERVER.noArg(), body) { index, count, data ->
            SharedSessionRequestMessage(requestId, index, count, data)
        }
    }

    /**
     * Asks [player]'s client to open the terminal at [terminal]. Server-only call: the caller has already
     * validated the block use, and the client side only opens a screen in reaction to this message.
     */
    fun openTerminal(player: ServerPlayer, terminal: JsonObject) {
        val instance = channel ?: error("SharedSessionNetwork.register() has not been called")
        val locator = terminal.toString()
        if (locator.length > MAX_LOCATOR_LENGTH) {
            throw IllegalArgumentException("The terminal locator is longer than $MAX_LOCATOR_LENGTH characters")
        }
        instance.send(playerTarget(player), SharedTerminalOpenMessage(locator))
    }

    /** Answers [player] with [payload] for [requestId]. Server-only; used by the request handler. */
    private fun respond(player: ServerPlayer, requestId: UUID, payload: JsonObject) {
        val instance = channel ?: return
        val body = payload.toString().toByteArray(Charsets.UTF_8)
        sendChunks(instance, playerTarget(player), body) { index, count, data ->
            SharedSessionResponseMessage(requestId, index, count, data)
        }
    }

    private fun handleRequest(message: SharedSessionRequestMessage, context: NetworkEvent.Context) {
        val player = context.sender ?: return
        val perPlayer = serverAssemblies.getOrPut(player.uuid) { HashMap() }
        val body = accept(perPlayer, message.requestId, message.chunkIndex, message.chunkCount, message.data)
        // A player keeps an entry here only while a request is mid-flight.
        if (perPlayer.isEmpty()) serverAssemblies.remove(player.uuid)
        if (body == null) return
        val request = parseJsonObject(body)
        if (request == null) {
            respond(player, message.requestId, failure("The request body is not a JSON object"))
            return
        }
        val operation = (request["operation"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (operation == null) {
            respond(player, message.requestId, failure("The request has no operation"))
            return
        }
        val payload = request["payload"] as? JsonObject ?: JsonObject(emptyMap())
        SharedSessionMechanism.handle(player, operation, payload) { response ->
            respond(player, message.requestId, response)
        }
    }

    private fun handleResponse(message: SharedSessionResponseMessage) {
        val body = accept(clientAssemblies, message.requestId, message.chunkIndex, message.chunkCount, message.data)
            ?: return
        val payload = parseJsonObject(body) ?: return
        MinecraftForge.EVENT_BUS.post(SharedSessionResponseEvent(message.requestId, payload))
    }

    private fun handleOpen(message: SharedTerminalOpenMessage) {
        val terminal = parseJsonObject(message.locator.toByteArray(Charsets.UTF_8)) ?: return
        MinecraftForge.EVENT_BUS.post(SharedTerminalOpenEvent(terminal))
    }

    /**
     * Feeds one chunk into the assemblies. Returns the complete body once the last chunk arrives, `null`
     * while more are outstanding, and `null` (dropping the assembly) when the framing is invalid or the bound
     * would be exceeded.
     */
    private fun accept(
        assemblies: MutableMap<UUID, ChunkAssembly>,
        requestId: UUID,
        chunkIndex: Int,
        chunkCount: Int,
        data: ByteArray,
    ): ByteArray? {
        val now = System.nanoTime()
        val iterator = assemblies.values.iterator()
        var pendingBytes = 0L
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (now - pending.lastChunkNanos > ASSEMBLY_IDLE_NANOS) iterator.remove()
            else pendingBytes += pending.bytes
        }
        if (pendingBytes + data.size > MAX_ASSEMBLED_BYTES) {
            assemblies.remove(requestId)
            return null
        }
        if (chunkCount !in 1..MAX_CHUNKS || chunkIndex !in 0 until chunkCount || data.size > MAX_CHUNK_BYTES) {
            LOGGER.warn("Dropping a malformed shared-session chunk ({}/{}), {} bytes", chunkIndex, chunkCount, data.size)
            assemblies.remove(requestId)
            return null
        }
        val existing = assemblies[requestId]
        if (existing == null && chunkIndex != 0) return null
        if (existing == null && assemblies.size >= MAX_PENDING_ASSEMBLIES) {
            LOGGER.warn("Dropping a shared-session request: too many assemblies are already in flight")
            return null
        }
        val assembly = existing ?: ChunkAssembly(chunkCount).also { assemblies[requestId] = it }
        val complete = try {
            assembly.accept(chunkIndex, chunkCount, data)
        } catch (invalid: IllegalArgumentException) {
            LOGGER.warn("Dropping a shared-session assembly: {}", invalid.message)
            assemblies.remove(requestId)
            return null
        }
        if (complete == null) return null
        assemblies.remove(requestId)
        return complete
    }

    private fun <M : Any> sendChunks(
        instance: SimpleChannel,
        target: PacketDistributor.PacketTarget,
        body: ByteArray,
        message: (index: Int, count: Int, data: ByteArray) -> M,
    ) {
        require(body.size.toLong() <= MAX_ASSEMBLED_BYTES) { "The shared-session payload is too large" }
        val count = maxOf(1, (body.size + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES)
        for (index in 0 until count) {
            val from = index * MAX_CHUNK_BYTES
            val to = minOf(body.size, from + MAX_CHUNK_BYTES)
            val data = if (from >= to) ByteArray(0) else body.copyOfRange(from, to)
            instance.send(target, message(index, count, data))
        }
    }

    private fun parseJsonObject(bytes: ByteArray): JsonObject? = try {
        Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)) as? JsonObject
    } catch (error: Exception) {
        LOGGER.warn("Discarding an unreadable shared-session payload")
        null
    }

    private fun playerTarget(player: ServerPlayer): PacketDistributor.PacketTarget =
        PacketDistributor.PLAYER.with { player }

    private fun failure(message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", message)
    }

    private class Chunk(val requestId: UUID, val index: Int, val count: Int, val data: ByteArray)

    private fun FriendlyByteBuf.writeChunk(requestId: UUID, index: Int, count: Int, data: ByteArray) {
        writeUUID(requestId)
        writeVarInt(index)
        writeVarInt(count)
        writeByteArray(data)
    }

    private fun FriendlyByteBuf.readChunk(): Chunk {
        val requestId = readUUID()
        val index = readVarInt()
        val count = readVarInt()
        val data = readByteArray(MAX_CHUNK_BYTES)
        return Chunk(requestId, index, count, data)
    }


    private const val ASSEMBLY_IDLE_NANOS = 30_000_000_000L
    /** Reassembles the ordered chunks of one body and enforces [MAX_ASSEMBLED_BYTES]. */
    private class ChunkAssembly(private val chunkCount: Int) {
        private var next = 0
        private val buffer = ByteArrayOutputStream()
        val bytes: Int get() = buffer.size()
        var lastChunkNanos: Long = System.nanoTime()
            private set

        fun accept(index: Int, count: Int, data: ByteArray): ByteArray? {
            if (count != chunkCount) throw IllegalArgumentException("the chunk count changed mid-assembly")
            if (index != next) throw IllegalArgumentException("chunks arrived out of order")
            if (buffer.size().toLong() + data.size > MAX_ASSEMBLED_BYTES) {
                throw IllegalArgumentException("the assembled payload exceeds $MAX_ASSEMBLED_BYTES bytes")
            }
            buffer.write(data)
            lastChunkNanos = System.nanoTime()
            next++
            return if (next == chunkCount) buffer.toByteArray() else null
        }
    }
}
