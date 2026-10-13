package com.rcdiamond.aifactory.addon.ae2

import appeng.api.storage.cells.CellState
import appeng.api.storage.cells.StorageCell
import com.rcdiamond.aifactory.shared.SharedCellStore
import com.rcdiamond.aifactory.shared.SharedSessionLimits
import java.util.UUID
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraftforge.server.ServerLifecycleHooks

/**
 * The mounted form of an intelligent cell, as AE2 sees it.
 *
 * This is the piece that makes the ME Drive honest. AE2 asks a mounted cell for its [getStatus] and
 * [getIdleDrain]; both are answered here:
 *
 * - the **status** is computed from the *real* occupancy of the cell's world-save sessions
 *   ([MEIntelligentCellUsage]) compared against the tier's byte capacity, so the drive LED is `EMPTY`,
 *   `NOT_EMPTY` or `FULL` because of stored conversations and nothing else. AE2 streams exactly this state
 *   to the client, so the LED is correct there too, and it is what the drive menu and the terminal screen
 *   report as `usedBytes` / `sessionCount`.
 * - the **idle drain** is the tier's AE2-equivalent value ([MEIntelligentCellTier.idleDrain]).
 *
 * ## No fake storage
 * [insert] and [extract] are left at their `MEStorage` defaults, which store and yield nothing, and
 * [getAvailableStacks] stays empty. An intelligent cell therefore can never hold an AE key, can never be
 * charged by an ME network, and cannot be used to smuggle resources into a drive: the byte figures the
 * network sees come from the session store, not from inserted items. `persist` is likewise a no-op, because
 * the only cell state in the stack is the reference, which is written through the drive's inventory when it
 * is assigned.
 */
class MEIntelligentCellInventory(
    private val item: MEIntelligentCellItem,
    private val stack: ItemStack,
) : StorageCell {

    /** The cell's reference, or `null` while the cell has never been discovered by a terminal. */
    private val cellId: UUID? get() = MEIntelligentCellRef.read(stack)

    /** ME byte capacity of this cell's tier. */
    private val capacityBytes: Long get() = item.tier.bytes

    /** ME bytes really charged by this cell's sessions; zero on the client or before discovery. */
    private val usedBytes: Long get() = cellId?.let { MEIntelligentCellUsage.usedBytes(it) } ?: 0L

    override fun getStatus(): CellState {
        val id = cellId ?: return CellState.EMPTY
        val used = usedBytes
        return when {
            used >= capacityBytes -> CellState.FULL
            MEIntelligentCellUsage.sessionCount(id) >= SharedSessionLimits.MAX_SESSIONS_PER_CELL -> CellState.TYPES_FULL
            used > 0L -> CellState.NOT_EMPTY
            else -> CellState.EMPTY
        }
    }

    override fun getIdleDrain(): Double = item.tier.idleDrain

    /**
     * Nothing to write. The sessions live in the world save, and the stack's only state - the reference - is
     * written through the drive's inventory when it is assigned, which already marks the drive changed and
     * saves it. Asking for another save here would only re-dirty the chunk from inside the block entity's
     * own save.
     */
    override fun persist() {
    }

    /** Never store an intelligent cell inside another cell: two stacks would address the same sessions. */
    override fun canFitInsideCell(): Boolean = false

    override fun getDescription(): Component = Component.translatable(item.descriptionId)
}

/**
 * Reads a cell's real occupancy out of the world save.
 *
 * The session document is attached to the overworld's data storage, so any server level resolves it; on the
 * client (or before a server exists) there is no world save and the answer is zero, which is what makes the
 * cell show up as `EMPTY` there. The client never displays this value anyway: AE2 sends it the state the
 * server computed.
 */
internal object MEIntelligentCellUsage {
    fun usedBytes(cellId: UUID): Long {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return 0L
        if (!server.isSameThread) return 0L
        return SharedCellStore.usedBytes(server.overworld(), cellId)
    }

    fun sessionCount(cellId: UUID): Int {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return 0
        if (!server.isSameThread) return 0
        return SharedCellStore.sessionCount(server.overworld(), cellId)
    }
}
