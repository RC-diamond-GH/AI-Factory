package com.rcdiamond.aifactory.addon.ae2.item

import com.rcdiamond.aifactory.addon.ae2.storage.MEIntelligentCellHandler
import com.rcdiamond.aifactory.addon.ae2.storage.MEIntelligentCellRef
import com.rcdiamond.aifactory.addon.ae2.storage.MEIntelligentCellTooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

/**
 * The intelligent storage cell item, one instance per [MEIntelligentCellTier].
 *
 * The item deliberately does *not* implement AE2's `IBasicCellItem`: that interface is handled by AE2's own
 * `BasicCellHandler`, which would turn the stack into a normal item cell that stores AE keys. An
 * intelligent cell stores no AE resources at all - only a reference to sessions in the world save - so it
 * is handled by [MEIntelligentCellHandler] and mounted by a drive through `StorageCells.isCellHandled`.
 *
 * The tooltip shows the GUI's short cell id, each saved title, then used AE2 bytes and session slots.
 * Its server-synchronized summary is display-only; the world-save store remains authoritative.
 */
class MEIntelligentCellItem(val tier: MEIntelligentCellTier) : Item(Item.Properties().stacksTo(1)) {

    /** Glint marks a cell that already carries an id, i.e. one that a terminal has discovered. */
    override fun isFoil(stack: ItemStack): Boolean = MEIntelligentCellRef.read(stack) != null

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag,
    ) {
        MEIntelligentCellTooltip.append(stack.tag, tier.bytes, tooltip)
    }
}
