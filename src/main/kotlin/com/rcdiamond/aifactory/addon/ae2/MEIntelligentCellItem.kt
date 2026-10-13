package com.rcdiamond.aifactory.addon.ae2

import com.rcdiamond.aifactory.shared.SharedSessionLimits
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
 * The tooltip is the honest summary of what the stack is: how much room the cell has, how many sessions fit,
 * and - only when advanced tooltips are on, and only once it has been discovered - the id that ties it to
 * the stored conversations.
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
        tooltip += Component.translatable(
            "tooltip.ai_factory.me_intelligent_cell.capacity",
            tier.bytes,
            tier.tokenBudget,
        )
        tooltip += Component.translatable(
            "tooltip.ai_factory.me_intelligent_cell.sessions",
            SharedSessionLimits.MAX_SESSIONS_PER_CELL,
        )

        val cellId = MEIntelligentCellRef.read(stack)
        if (cellId == null) {
            tooltip += Component.translatable("tooltip.ai_factory.me_intelligent_cell.empty")
        } else {
            tooltip += Component.translatable("tooltip.ai_factory.me_intelligent_cell.reference")
            if (flag.isAdvanced) {
                tooltip += Component.translatable("tooltip.ai_factory.me_intelligent_cell.id", cellId.toString())
            }
        }
    }
}
