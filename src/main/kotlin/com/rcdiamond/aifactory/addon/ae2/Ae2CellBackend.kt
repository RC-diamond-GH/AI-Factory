package com.rcdiamond.aifactory.addon.ae2

import appeng.api.implementations.blockentities.IChestOrDrive
import appeng.blockentity.AEBaseBlockEntity
import appeng.blockentity.AEBaseInvBlockEntity
import appeng.blockentity.storage.ChestBlockEntity
import com.rcdiamond.aifactory.shared.SharedCell
import com.rcdiamond.aifactory.shared.SharedTerminalBackend
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * The addon half of the shared session bridge.
 *
 * Installed into `SharedSessionService.backend` during the addon bootstrap; from then on the core shared
 * session service can ask two questions:
 *
 * - [cells]: which intelligent cells are *really* mounted in this terminal's grid. Every active store on the
 *   grid that can hold cells (an ME Drive, or an ME Chest) is walked, every slot is inspected for one of our
 *   items, and a cell that has never been seen gets its reference id written into its stack through the
 *   drive's own inventory - so AE2 re-reads the cell, the drive is marked changed and the modified stack is
 *   saved with the drive. Nothing is cached here: the world save is the single source of truth, which is why
 *   a reload or a moved drive cannot make the list disagree with reality.
 * - [execute]: forward one tool operation to the tool executor for a player the core has already verified
 *   holds the session lease.
 *
 * ## Fail-closed behaviour
 * A malformed locator, an unloaded chunk, a missing terminal block, a dead network or a forbidden player all
 * raise [IllegalArgumentException] with a message that is safe to show the requesting player; the core turns
 * that into `{"ok":false,"error":...}` and nothing is listed, opened or executed. With `allowRemote` the
 * distance bound is dropped (a background turn keeps working while the screen is closed), but dimension,
 * load, active grid and interaction permission never are.
 */
object Ae2CellBackend : SharedTerminalBackend {

    override fun cells(player: ServerPlayer, terminal: JsonObject, allowRemote: Boolean): List<SharedCell> {
        val located = locate(player, terminal)
        val grid = located.terminal.resolve(player, allowRemote)

        val mounted = ArrayList<SharedCell>()
        val touchedDrives = ArrayList<AEBaseBlockEntity>()

        for (node in grid.nodes) {
            if (!node.isActive) {
                continue
            }
            val drive = node.owner as? IChestOrDrive ?: continue
            val inventoryOwner = drive as? AEBaseInvBlockEntity ?: continue
            val inventory = inventoryOwner.internalInventory

            var foundAny = false
            val slots = drive.cellCount
            for (slot in 0 until slots) {
                // An ME chest exposes [item input, cell], whereas a drive exposes only cell slots.
                val inventorySlot = if (drive is ChestBlockEntity) slot + 1 else slot
                val stack = inventory.getStackInSlot(inventorySlot)
                val cellItem = stack.item as? MEIntelligentCellItem ?: continue
                val cellId = MEIntelligentCellRef.read(stack)
                    ?: MEIntelligentCellRef.assign(inventory, inventorySlot, stack)
                mounted += SharedCell(cellId, cellItem.tier.bytes)
                foundAny = true
            }

            if (foundAny) {
                touchedDrives += inventoryOwner
            }
        }

        refreshMountedCellStatus(touchedDrives)
        return mounted
    }

    override fun execute(
        player: ServerPlayer,
        terminal: JsonObject,
        operation: String,
        arguments: JsonObject,
        reply: (JsonObject) -> Unit,
    ) {
        val located = locate(player, terminal)
        // The core only dispatches a tool for a player who holds the session lease, so the distance bound is
        // relaxed here for the same reason as in save/state: a background turn keeps running while the screen
        // is closed. Everything else - dimension, load, active grid, interaction permission - still applies.
        located.terminal.resolve(player, allowRemote = true)
        Ae2ToolExecutor.execute(player, located.terminal, operation, arguments, reply)
    }

    /**
     * A cell's status is derived from the world-save sessions, so AE2 never gets the insert/extract that
     * would normally make a drive re-render its cell LED. Asking the drives that hold our cells for an
     * update - on every terminal interaction, which is also when a session may just have been written -
     * keeps the streamed cell state, and with it the drive's occupancy display, consistent with the real
     * usage.
     */
    private fun refreshMountedCellStatus(drives: List<AEBaseBlockEntity>) {
        for (drive in drives) {
            if (!drive.isRemoved) {
                drive.markForUpdate()
            }
        }
    }

    private fun locate(player: ServerPlayer, locator: JsonObject): LocatedTerminal {
        val dimension = locator.string("dimension")
        val levelKey = ResourceLocation.tryParse(dimension)
            ?.let { ResourceKey.create(Registries.DIMENSION, it) }
            ?: throw IllegalArgumentException(ERROR_TERMINAL)
        val level = player.server.getLevel(levelKey) ?: throw IllegalArgumentException(ERROR_TERMINAL)
        val pos = BlockPos(locator.int("x"), locator.int("y"), locator.int("z"))
        if (player.serverLevel() !== level || !level.isLoaded(pos)) {
            throw IllegalArgumentException(MEIntelligentTerminalBlockEntity.ERROR_NOT_LOADED)
        }

        val terminal = level.getBlockEntity(pos) as? MEIntelligentTerminalBlockEntity
            ?: throw IllegalArgumentException(MEIntelligentTerminalBlockEntity.ERROR_NOT_LOADED)
        return LocatedTerminal(terminal)
    }

    private fun JsonObject.string(name: String): String {
        val element = this[name] as? JsonPrimitive ?: throw IllegalArgumentException(ERROR_TERMINAL)
        if (!element.isString) throw IllegalArgumentException(ERROR_TERMINAL)
        return element.content
    }

    private fun JsonObject.int(name: String): Int {
        val element = this[name] as? JsonPrimitive ?: throw IllegalArgumentException(ERROR_TERMINAL)
        return element.intOrNull ?: throw IllegalArgumentException(ERROR_TERMINAL)
    }

    /** A terminal resolved from a request locator. */
    private class LocatedTerminal(val terminal: MEIntelligentTerminalBlockEntity)

    /** Safe message for a locator that cannot be resolved to a terminal. */
    const val ERROR_TERMINAL: String = "That intelligent terminal is not available"
}
