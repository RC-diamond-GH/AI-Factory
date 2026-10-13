package com.rcdiamond.aifactory.addon.ae2.storage

import appeng.api.storage.cells.ICellHandler
import appeng.api.storage.cells.ISaveProvider
import appeng.api.storage.cells.StorageCell
import com.rcdiamond.aifactory.addon.ae2.Ae2Addon
import com.rcdiamond.aifactory.addon.ae2.item.MEIntelligentCellItem
import com.rcdiamond.aifactory.addon.ae2.server.Ae2CellBackend
import net.minecraft.world.item.ItemStack

/**
 * Makes an intelligent cell a first-class AE2 storage cell without giving it any AE storage.
 *
 * AE2 asks its registered handlers whether an item is a cell (`StorageCells.isCellHandled`, used by drive
 * slots) and which inventory it stands for (`StorageCells.getCellInventory`, used whenever a drive, chest or
 * portable cell mounts it). Answering both here is what lets a normal ME Drive accept the item and lets its
 * status LED, idle drain and mounted-cell membership come from us.
 *
 * Registered once from [Ae2Addon] during common setup, after AE2 has installed its own handlers.
 */
object MEIntelligentCellHandler : ICellHandler {

    override fun isCell(stack: ItemStack): Boolean = stack.item is MEIntelligentCellItem

    /**
     * The inventory representation of [stack].
     *
     * The stack handed over is AE2's own copy, so it is kept only to read the reference out of it; anything
     * that has to be written back (assigning a fresh id) goes through the drive's inventory in
     * [Ae2CellBackend]. `saveProvider` - the block entity that mounts the cell - is deliberately not needed:
     * a mounted intelligent cell never writes anything itself, so it never has to ask for a save.
     */
    override fun getCellInventory(stack: ItemStack, saveProvider: ISaveProvider?): StorageCell? {
        val item = stack.item as? MEIntelligentCellItem ?: return null
        return MEIntelligentCellInventory(item, stack)
    }
}
