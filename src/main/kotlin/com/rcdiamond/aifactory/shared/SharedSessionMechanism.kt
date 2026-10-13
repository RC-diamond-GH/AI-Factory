package com.rcdiamond.aifactory.shared

import com.mojang.logging.LogUtils
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStoppedEvent

/**
 * Server-authoritative implementation of the shared-session RPC operations.
 *
 * Every request runs on the server main thread with the [ServerPlayer] the network layer resolved from the
 * connection; no player, session or cell id is ever taken on trust from the payload. The flow is:
 *
 * - `list` / `create` / `open` require the terminal to be reachable *now* (`allowRemote = false`): the
 *   player is at the terminal, so a drive in an unloaded chunk or another dimension is refused by the
 *   backend.
 * - `save` / `state` / `tool` are only accepted from a player who holds the session's lease, and then ask
 *   the backend for the mounted cells with `allowRemote = true`: a background turn keeps running while the
 *   screen is closed, so distance no longer matters, but dimension, load, active grid and interaction
 *   permission still do.
 *
 * A rejected request answers `{"ok":false,"error":<safe message>}`; a successful one
 * `{"ok":true,...}`. `IllegalArgumentException` from the backend is a rejection (its message is shown),
 * any other failure is logged and reported as a generic error so no internals leak. Each operation's exact
 * payload and reply shape is documented on [handle].
 */
object SharedSessionMechanism {
    private val LOGGER = LogUtils.getLogger()

    /** Tool operations the core forwards to the backend; anything else is rejected before it is dispatched. */
    val TOOL_OPERATIONS: Set<String> = setOf(
        "ae2_read_inventory", "ae2_item_details", "ae2_crafting_plan",
        "ae2_request_crafting", "ae2_manage_patterns",
    )

    /** The live leases; kept private so only this object's requests can mutate the lock state. */
    private val leases = SharedSessionLeases()

    /**
     * Hooks the runtime-only lease cleanup into the game bus: a player who logs out frees their session,
     * and a stopped server drops every lease. Registered once from
     * [com.rcdiamond.aifactory.network.SharedSessionNetwork.register].
     */
    fun install() {
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> { event ->
            leases.release(event.entity.uuid)
        }
        MinecraftForge.EVENT_BUS.addListener<ServerStoppedEvent> { leases.clear() }
    }

    /**
     * Handles one assembled request. [reply] is called exactly once, from the server thread — immediately for
     * the synchronous operations and later (on a tick) for a tool that polls work.
     *
     * The `terminal` field of a payload is the locator `{dimension,x,y,z,side}`; the sender is always the
     * connection's own player and never a value from [payload].
     *
     * - `list` `{terminal}` → `{ok,sessions:[{id,cellId,title,autoTitle,hash,tokens}],cells:[{id,bytes,usedBytes,sessionCount}]}`,
     *   restricted to the cells the backend reports as installed in that terminal.
     * - `create` `{terminal,cellId,title,autoTitle}` → `{ok,id,cellId}`. Prefers the requested installed cell, then
     *   another installed cell with room. Each cell holds at most [SharedSessionLimits.MAX_SESSIONS_PER_CELL]
     *   sessions within its byte budget. The response names the actual cell used.
     * - `open` `{terminal,id,hash}` → `{ok,id,title,autoTitle,hash,tokens}` plus `context` **only** when the client's `hash`
     *   differs from the server's. Acquires the exclusive lease atomically; a player may reopen the session
     *   they already own, anything else is a conflict. The acquired state is `screenOpen = true, running = false`.
     * - `save` `{id,hash,context,title?}` → `{ok,title,autoTitle,hash,tokens}`. Requires the caller's lease, a
     *   still-mounted cell and an unchanged `hash`; context and the optional first generated title are saved atomically.
     * - `delete` `{terminal,id,hash}` → `{ok}`. Removes the session from the store, freeing its slot and its ME
     *   bytes. Only accepted while the session's cell is installed in that terminal; `hash` must still be the
     *   one the client listed, so a stale listing cannot destroy a newer conversation. A session held by
     *   another player, or one whose turn is still running (even the caller's own), is refused. The caller's
     *   own idle lease is dropped only after the store mutation succeeds, and nothing is flushed.
     * - `state` `{id,screenOpen,running}` → `{ok}`. Requires the lease; the lease is released exactly when both
     *   flags are false. The client must have its save acknowledged before it reports `running = false`.
     * - `tool` `{id,operation,arguments}` → whatever the backend answers, passed through unchanged. Requires the
     *   lease, a mounted cell and an operation in [TOOL_OPERATIONS]; `ae2_crafting_plan` returns a *plan*, it
     *   never submits a crafting job.
     */
    fun handle(player: ServerPlayer, operation: String, payload: JsonObject, reply: (JsonObject) -> Unit) {
        val backend = SharedSessionService.backend
        if (backend == null) {
            reply(error("The shared terminal is not available on this server"))
            return
        }
        try {
            val data = SharedSessionSavedData.get(player.server)
            when (operation) {
                "list" -> list(player, payload, backend, data, reply)
                "create" -> create(player, payload, backend, data, reply)
                "open" -> open(player, payload, backend, data, reply)
                "save" -> save(player, payload, backend, data, reply)
                "delete" -> delete(player, payload, backend, data, reply)
                "state" -> state(player, payload, data, reply)
                "tool" -> tool(player, payload, backend, data, reply)
                else -> reply(error("Unknown operation '$operation'"))
            }
        } catch (rejected: IllegalArgumentException) {
            reply(error(rejected.message ?: "The request was rejected"))
        } catch (failure: Exception) {
            LOGGER.error("Shared session operation '{}' failed", operation, failure)
            reply(error("The request could not be completed"))
        }
    }

    private fun list(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val terminal = locator(payload)
        val cells = backend.cells(player, terminal, false)
        val cellIds = cells.mapTo(HashSet()) { it.id }
        val store = data.store
        val sessions = store.list(cellIds).map { record ->
            buildJsonObject {
                put("id", record.id.toString())
                put("cellId", record.cellId.toString())
                put("title", record.title)
                put("autoTitle", record.autoTitle)
                put("hash", record.hash)
                put("tokens", record.tokens)
            }
        }
        val cellsJson = cells.map { cell ->
            buildJsonObject {
                put("id", cell.id.toString())
                put("bytes", cell.bytes)
                put("usedBytes", store.usedBytes(cell.id))
                put("sessionCount", store.sessionCount(cell.id))
            }
        }
        reply(
            ok {
                put("sessions", JsonArray(sessions))
                put("cells", JsonArray(cellsJson))
            },
        )
    }

    private fun create(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val terminal = locator(payload)
        val cellId = payload.requireUuid("cellId")
        val title = payload.requireString("title")
        val cells = backend.cells(player, terminal, false)
        val record = data.store.createInMountedCell(cells, cellId, title, payload.requireBoolean("autoTitle"))
        data.markChanged()
        reply(ok {
            put("id", record.id.toString())
            put("cellId", record.cellId.toString())
        })
    }

    private fun open(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val terminal = locator(payload)
        val id = payload.requireUuid("id")
        val localHash = payload.stringOrNull("hash") ?: ""
        val record = data.store.find(id) ?: throw IllegalArgumentException("No such session")
        if (backend.cells(player, terminal, false).none { it.id == record.cellId }) {
            throw IllegalArgumentException("That session's cell is not installed in this terminal")
        }
        leases.acquire(player.uuid, record.id, record.cellId, terminal)
        reply(
            ok {
                put("id", record.id.toString())
                put("title", record.title)
                put("autoTitle", record.autoTitle)
                put("hash", record.hash)
                put("tokens", record.tokens)
                // The body is sent only when the client's cache is stale; a matching hash means it can reuse
                // what it already has.
                if (localHash != record.hash) put("context", record.context)
            },
        )
    }

    private fun save(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val id = payload.requireUuid("id")
        val expectedHash = payload.requireString("hash")
        val context = payload["context"] as? JsonObject
            ?: throw IllegalArgumentException("Field 'context' must be an object")
        val lease = requireLease(player, id)
        // The optimistic guard runs before anything is canonicalised or charged.
        val record = data.store.requireFresh(id, expectedHash)
        val cell = backend.cells(player, lease.terminal, true).firstOrNull { it.id == record.cellId }
            ?: throw IllegalArgumentException("That session's cell is not mounted")
        val saved = data.store.replace(id, context, cell.bytes, payload.stringOrNull("title"))
        data.markChanged()
        reply(
            ok {
                put("hash", saved.hash)
                put("tokens", saved.tokens)
                put("title", saved.title)
                put("autoTitle", saved.autoTitle)
            },
        )
    }

    private fun delete(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val terminal = locator(payload)
        val id = payload.requireUuid("id")
        val expectedHash = payload.requireString("hash")
        // The lock rules run before anything is destroyed: another player's session and any running turn are
        // never touched, while the caller's own idle lease does not stand in the way of deleting it.
        val lease = leases.authorizeDeletion(player.uuid, id)
        val record = data.store.find(id) ?: throw IllegalArgumentException("No such session")
        // Only a session whose cell is installed in this terminal right now may be deleted; the store's own
        // hash guard then rejects a stale listing instead of destroying a newer conversation.
        if (backend.cells(player, terminal, false).none { it.id == record.cellId }) {
            throw IllegalArgumentException("That session's cell is not installed in this terminal")
        }
        data.store.delete(id, expectedHash)
        data.markChanged()
        // The lease is only dropped once the store really changed, so a refused deletion keeps the session held.
        if (lease != null) leases.release(player.uuid)
        reply(ok {})
    }

    private fun state(
        player: ServerPlayer,
        payload: JsonObject,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val id = payload.requireUuid("id")
        val screenOpen = payload.requireBoolean("screenOpen")
        val running = payload.requireBoolean("running")
        val lease = requireLease(player, id)
        // The cell must still be mounted in the terminal the session was opened through; the distance bound
        // is relaxed so a background turn can report its end while the player is away.
        requireMountedCell(player, data, lease, id)
        lease.update(screenOpen, running)
        if (lease.shouldRelease()) leases.release(player.uuid)
        reply(ok {})
    }

    private fun tool(
        player: ServerPlayer,
        payload: JsonObject,
        backend: SharedTerminalBackend,
        data: SharedSessionSavedData,
        reply: (JsonObject) -> Unit,
    ) {
        val id = payload.requireUuid("id")
        val operation = payload.requireString("operation")
        if (operation !in TOOL_OPERATIONS) throw IllegalArgumentException("Unknown tool operation '$operation'")
        val arguments = when (val element = payload["arguments"]) {
            null -> JsonObject(emptyMap())
            is JsonObject -> element
            else -> throw IllegalArgumentException("Field 'arguments' must be an object")
        }
        val lease = requireLease(player, id)
        val authorize = {
            if (requireLease(player, id) !== lease) {
                throw IllegalArgumentException("The session lock changed while the tool was running")
            }
            requireMountedCell(player, data, lease, id)
        }
        authorize()
        backend.execute(player, lease.terminal, operation, arguments, authorize) { response ->
            try {
                authorize()
                reply(response)
            } catch (rejected: IllegalArgumentException) {
                reply(error(rejected.message ?: "The session is no longer available"))
            }
        }
    }

    /**
     * Confirms the session's cell is still one of the cells the backend currently reports as mounted in the
     * terminal the lease was taken through. The distance bound is relaxed (`allowRemote = true`) because this
     * runs after the lease has already been verified.
     */
    private fun requireMountedCell(
        player: ServerPlayer,
        data: SharedSessionSavedData,
        lease: SharedSessionLease,
        sessionId: UUID,
    ) {
        val backend = SharedSessionService.backend
            ?: throw IllegalArgumentException("The shared terminal is not available on this server")
        val cellIds = backend.cells(player, lease.terminal, true).mapTo(HashSet()) { it.id }
        if (!data.store.isMountedIn(sessionId, cellIds)) {
            throw IllegalArgumentException("That session's cell is not mounted")
        }
    }

    private fun requireLease(player: ServerPlayer, sessionId: UUID): SharedSessionLease {
        val lease = leases.leaseFor(player.uuid)
            ?: throw IllegalArgumentException("You do not have this session open")
        if (lease.sessionId != sessionId) throw IllegalArgumentException("You do not have this session open")
        return lease
    }

    /**
     * Validates the terminal locator `{dimension,x,y,z,side}` and returns its canonical form. Only the shape
     * is checked here; whether the position really holds a usable terminal is the backend's decision. The
     * side is part of the shape: a request that does not name the exact face is rejected instead of being
     * resolved to some default one.
     */
    private fun locator(payload: JsonObject): JsonObject {
        val locator = payload["terminal"] as? JsonObject
            ?: throw IllegalArgumentException("Field 'terminal' must be an object")
        return SharedTerminalLocator.parse(locator).toJson()
    }

    private fun ok(body: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put("ok", true)
        body()
    }

    private fun error(message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", message)
    }
}
