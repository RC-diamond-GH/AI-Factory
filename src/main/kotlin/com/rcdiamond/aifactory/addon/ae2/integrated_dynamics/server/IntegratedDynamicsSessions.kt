package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.block.IntegratedDynamicsTerminalBlockEntity
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.shared.SharedSessionLease
import com.rcdiamond.aifactory.shared.SharedSessionLeases
import com.rcdiamond.aifactory.shared.requireString
import com.rcdiamond.aifactory.shared.requireUuid
import com.rcdiamond.aifactory.shared.stringOrNull
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.cyclops.integrateddynamics.api.network.INetwork
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers

/** ID owns separate stores and leases. Only the bounded transport and context format are shared with ME. */
object IntegratedDynamicsSessions {
    const val MAX_SESSIONS_PER_TERMINAL = IntegratedDynamicsSessionStore.MAX_SESSIONS_PER_TERMINAL
    private val logger = LogUtils.getLogger()
    private val leases = SharedSessionLeases()
    private val LOCATOR_FIELDS = setOf("kind", "dimension", "x", "y", "z")

    fun install() {
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> { leases.release(it.entity.uuid) }
        MinecraftForge.EVENT_BUS.addListener<ServerStoppedEvent> { leases.clear() }
    }

    /** Server-side access to one of this block's persisted references; never a global UUID lookup. */
    fun find(
        level: ServerLevel,
        terminal: IntegratedDynamicsTerminalBlockEntity,
        id: UUID,
    ): IntegratedDynamicsSessionRecord? {
        check(level.server.isSameThread) { "Sessions must be accessed on the server main thread" }
        if (terminal.level !== level || terminal.isRemoved || !level.isLoaded(terminal.blockPos) ||
            level.getBlockEntity(terminal.blockPos) !== terminal
        ) return null
        if (!terminal.hasSession(id)) return null
        return IntegratedDynamicsSessionSavedData.get(level.server).store.find(terminal.terminalId, id)
    }

    /** Network transport strips id_ before entering this independent endpoint. */
    fun handle(player: ServerPlayer, operation: String, payload: JsonObject, reply: (JsonObject) -> Unit) {
        try {
            val data = IntegratedDynamicsSessionSavedData.get(player.server)
            val store = data.store
            val response = when (operation) {
                "list" -> {
                    val terminal = resolve(player, locator(payload), false)
                    val records = terminal.sessionIds.mapNotNull { store.find(terminal.terminalId, it) }
                    ok {
                        put("terminalId", terminal.terminalId.toString())
                        put("sessions", JsonArray(records.map { metadata(it) }))
                    }
                }
                "create" -> {
                    val terminal = resolve(player, locator(payload), false)
                    require(payload.requireUuid("terminalId") == terminal.terminalId) { "The terminal was replaced; refresh its sessions" }
                    require(terminal.sessionCount < MAX_SESSIONS_PER_TERMINAL) { "This terminal has too many session references" }
                    val record = store.create(terminal.terminalId, payload.requireString("title"), payload.boolean("autoTitle"))
                    terminal.addSession(record.id)
                    data.setDirty()
                    ok { put("id", record.id.toString()); put("terminalId", terminal.terminalId.toString()) }
                }
                "open" -> {
                    val address = locator(payload)
                    val terminal = resolve(player, address, false)
                    val id = payload.requireUuid("id")
                    val record = requireSession(terminal, store, id)
                    leases.acquire(player.uuid, id, terminal.terminalId, address)
                    ok {
                        for ((key, value) in metadata(record)) put(key, value)
                        if (payload.stringOrNull("hash") != record.hash) put("context", record.context)
                    }
                }
                "save" -> {
                    val id = payload.requireUuid("id")
                    val lease = requireLease(player, id)
                    val terminal = resolveLeased(player, lease, store)
                    val context = payload["context"] as? JsonObject ?: throw IllegalArgumentException("Field 'context' must be an object")
                    val record = store.replace(
                        terminal.terminalId, id, payload.requireString("hash"), context, payload.stringOrNull("title"),
                    )
                    data.setDirty()
                    ok { for ((key, value) in metadata(record)) put(key, value) }
                }
                "delete" -> {
                    val id = payload.requireUuid("id")
                    val lease = leases.authorizeDeletion(player.uuid, id)
                    val terminal = resolve(player, locator(payload), false)
                    requireSession(terminal, store, id)
                    store.delete(terminal.terminalId, id, payload.requireString("hash"))
                    terminal.removeSession(id)
                    data.setDirty()
                    if (lease != null) leases.release(player.uuid)
                    ok {}
                }
                "state" -> {
                    val lease = requireLease(player, payload.requireUuid("id"))
                    // Releasing never grants access and must remain possible after a cable is removed or a
                    // chunk unloads, otherwise an idle client cannot surrender its stranded lease.
                    val screenOpen = payload.boolean("screenOpen")
                    val running = payload.boolean("running")
                    if (screenOpen || running) resolveLeased(player, lease, store)
                    lease.update(screenOpen, running)
                    if (lease.shouldRelease()) leases.release(player.uuid)
                    ok {}
                }
                "tool" -> {
                    val lease = requireLease(player, payload.requireUuid("id"))
                    val terminal = resolveLeased(player, lease, store)
                    val tool = payload.requireString("operation")
                    require(tool in IntegratedDynamicsToolLimits.OPERATIONS) { "Unknown Integrated Dynamics tool '$tool'" }
                    val arguments = payload["arguments"] as? JsonObject
                        ?: throw IllegalArgumentException("Field 'arguments' must be an object")
                    val network = terminal.resolve(player, true)
                    when (tool) {
                        IntegratedDynamicsToolLimits.OPERATION_DESCRIBE_INPUTS ->
                            IdNativeBackend.describe(partNetwork(network), serverLevel(terminal), arguments)
                        IntegratedDynamicsToolLimits.OPERATION_CREATE_CARDS ->
                            IdNativeBackend.create(player, partNetwork(network), serverLevel(terminal), arguments)
                        else -> IntegratedDynamicsNetworkAccess.inspect(network, tool, arguments)
                    }
                }
                else -> throw IllegalArgumentException("Unknown Integrated Dynamics session operation '$operation'")
            }
            reply(response)
        } catch (rejected: IdBackendError) {
            // Preserve phase/code/node/input and any cards that were already placed; a plain
            // IllegalArgumentException reply would lose exactly the information needed to recover.
            reply(rejected.toJson())
        } catch (rejected: IllegalArgumentException) {
            reply(buildJsonObject { put("ok", false); put("error", rejected.message ?: "The request was rejected") })
        } catch (failure: Exception) {
            logger.error("Integrated Dynamics session operation '{}' failed", operation, failure)
            reply(buildJsonObject { put("ok", false); put("error", "The Integrated Dynamics request could not be completed") })
        }
    }

    /** The terminal's own authorized network must be part-network capable for aspect/variable access. */
    private fun partNetwork(network: INetwork): IPartNetwork =
        NetworkHelpers.getPartNetwork(network).resolve().getOrNull()
            ?: throw IllegalArgumentException("This Integrated Dynamics network has no part network")

    private fun serverLevel(terminal: IntegratedDynamicsTerminalBlockEntity): ServerLevel =
        terminal.level as? ServerLevel
            ?: throw IllegalArgumentException("The Integrated Dynamics terminal is not in a server level")

    private fun requireLease(player: ServerPlayer, id: UUID): SharedSessionLease {
        val lease = leases.leaseFor(player.uuid)
        require(lease != null && lease.sessionId == id) { "You do not have this Integrated Dynamics session open" }
        return lease
    }

    private fun resolveLeased(
        player: ServerPlayer, lease: SharedSessionLease, store: IntegratedDynamicsSessionStore,
    ): IntegratedDynamicsTerminalBlockEntity {
        val terminal = resolve(player, lease.terminal, true)
        require(terminal.terminalId == lease.cellId) { "The terminal that owned this session was replaced" }
        requireSession(terminal, store, lease.sessionId)
        return terminal
    }

    private fun requireSession(
        terminal: IntegratedDynamicsTerminalBlockEntity, store: IntegratedDynamicsSessionStore, id: UUID,
    ): IntegratedDynamicsSessionRecord {
        require(terminal.hasSession(id)) { "This terminal does not reference that session" }
        return store.find(terminal.terminalId, id) ?: throw IllegalArgumentException("No such session on this terminal")
    }

    private fun locator(payload: JsonObject): JsonObject {
        val address = payload["terminal"] as? JsonObject ?: throw IllegalArgumentException("Field 'terminal' must be an object")
        require(address.keys.all { it in LOCATOR_FIELDS }) { "Unknown terminal locator field" }
        require(address.requireString("kind") == "integrated_dynamics") { "This is not an Integrated Dynamics terminal" }
        val dimension = ResourceLocation.tryParse(address.requireString("dimension"))
            ?: throw IllegalArgumentException("Invalid terminal dimension")
        return buildJsonObject {
            put("kind", "integrated_dynamics")
            put("dimension", dimension.toString())
            put("x", address.integer("x")); put("y", address.integer("y")); put("z", address.integer("z"))
        }
    }

    private fun resolve(player: ServerPlayer, address: JsonObject, allowRemote: Boolean): IntegratedDynamicsTerminalBlockEntity {
        val level = player.serverLevel()
        require(level.dimension().location().toString() == address.requireString("dimension")) { "That terminal is in another dimension" }
        val pos = BlockPos(address.integer("x"), address.integer("y"), address.integer("z"))
        require(level.isLoaded(pos)) { "That Integrated Dynamics terminal is not loaded" }
        val terminal = level.getBlockEntity(pos) as? IntegratedDynamicsTerminalBlockEntity
            ?: throw IllegalArgumentException("That block is not an Integrated Dynamics terminal")
        terminal.resolve(player, allowRemote)
        return terminal
    }

    private fun metadata(record: IntegratedDynamicsSessionRecord): JsonObject = buildJsonObject {
        put("id", record.id.toString()); put("terminalId", record.terminalId.toString())
        put("title", record.title); put("autoTitle", record.autoTitle)
        put("hash", record.hash); put("tokens", record.tokens)
    }

    private fun ok(body: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject { put("ok", true); body() }

    private fun JsonObject.boolean(field: String): Boolean =
        (this[field] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
            ?: throw IllegalArgumentException("Field '$field' must be a boolean")

    private fun JsonObject.integer(field: String): Int =
        (this[field] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            ?: throw IllegalArgumentException("Field '$field' must be an integer")
}
