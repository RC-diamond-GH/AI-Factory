package com.rcdiamond.aifactory.shared

import java.util.UUID
import kotlinx.serialization.json.JsonObject
import net.minecraft.server.level.ServerPlayer

/**
 * One intelligent ME cell installed in a terminal's grid, seen through the addon-owned backend.
 *
 * [id] is the cell's stable identity, assigned into the cell's `ItemStack` NBT by the backend the first
 * time it is read; [bytes] is its ME byte capacity. The core shared package never references an AE2 type:
 * a cell reaching the core is just an id and a capacity.
 */
data class SharedCell(val id: UUID, val bytes: Long)

/**
 * The only bridge the core shared session service has into the AE2 addon.
 *
 * Implemented by the addon (`addon.ae2`) and installed into [SharedSessionService.backend] during the
 * addon's bootstrap. Both methods are called on the server main thread with the sender the core resolved
 * from the connection — the core never trusts a player id, cell id or position carried in a request.
 *
 * [cells] must validate the terminal before returning anything and must return the *actual* drive-installed
 * intelligent cells of that terminal's grid, assigning an id through the cell stack's NBT and marking the
 * drive changed when it does. With `allowRemote = false` the player must be loaded, in the same dimension,
 * within 8 blocks and allowed to interact; with `allowRemote = true` only the distance bound is relaxed —
 * dimension, load, active grid and interaction permission still apply. The core only ever passes
 * `allowRemote = true` after it has verified the caller holds the session lease.
 *
 * [execute] runs one tool operation for a lock holder. It may answer asynchronously (crafting planning is
 * polled on a server tick) through [reply]; the reply is a payload object following the
 * `{"ok":true,...}` / `{"ok":false,"error":...}` convention. Call [authorize] immediately before
 * asynchronous side effects to revalidate the original lease and mounted cell.
 *
 * Both methods report a rejected request by throwing [IllegalArgumentException] with a message that is safe
 * to show the requesting player (no stack traces, paths or internal ids beyond what the player supplied).
 */
interface SharedTerminalBackend {
    fun cells(player: ServerPlayer, terminal: JsonObject, allowRemote: Boolean): List<SharedCell>

    fun execute(
        player: ServerPlayer,
        terminal: JsonObject,
        operation: String,
        arguments: JsonObject,
        authorize: () -> Unit,
        reply: (JsonObject) -> Unit,
    )
}

/**
 * Holder the addon installs its [SharedTerminalBackend] into.
 *
 * Read on every request, so a `null` backend (AE2 absent, or the addon not yet bootstrapped) makes every
 * operation answer with a fail-closed error instead of throwing.
 */
object SharedSessionService {
    @Volatile
    var backend: SharedTerminalBackend? = null
}
