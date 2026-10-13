package com.rcdiamond.aifactory.addon.ae2.storage

import appeng.api.inventories.InternalInventory
import java.util.UUID
import net.minecraft.world.item.ItemStack

/**
 * The cell reference links an intelligent cell's `ItemStack` to the world-save sessions.
 *
 * The conversation data itself lives in the core shared store. The stack carries the id and a display-only
 * tooltip summary of titles and usage, never context bodies or authoritative capacity bookkeeping.
 * A duplicated stack still refers to the same sessions instead of cloning them, which is why
 * [MEIntelligentCellInventory.canFitInsideCell] refuses to store a cell inside another cell.
 *
 * An id is assigned lazily the first time a mounted cell is discovered through a terminal, which is exactly
 * when [assign] is called: it writes the id through the drive's own inventory, so AE2 re-reads the cell,
 * marks the block entity changed and persists the new stack as part of the drive.
 */
internal object MEIntelligentCellRef {
    /** NBT key of the reference; namespaced so it can never collide with an AE2 cell tag. */
    const val TAG_CELL_ID: String = "ai_factory:cell_id"

    /** The id stored in [stack], or `null` when this cell has not been discovered yet. */
    fun read(stack: ItemStack): UUID? {
        val tag = stack.tag ?: return null
        return if (tag.hasUUID(TAG_CELL_ID)) tag.getUUID(TAG_CELL_ID) else null
    }

    /** Writes [id] into [stack]'s tag. */
    fun write(stack: ItemStack, id: UUID) {
        stack.orCreateTag.putUUID(TAG_CELL_ID, id)
    }

    /**
     * Gives the cell in [slot] a fresh id and stores it through [inventory].
     *
     * Going through the inventory is deliberate: `InternalInventory.setItemDirect` replaces the stack,
     * which makes the drive re-create the cell inventory from the updated stack, mark itself for a client
     * update and queue a world save, so the id is durable even if the server stops right afterwards.
     */
    fun assign(inventory: InternalInventory, slot: Int, stack: ItemStack): UUID {
        val id = UUID.randomUUID()
        val updated = stack.copy()
        write(updated, id)
        inventory.setItemDirect(slot, updated)
        return id
    }
}
