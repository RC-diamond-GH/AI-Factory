package com.rcdiamond.aifactory.addon.ae2.storage

import com.rcdiamond.aifactory.addon.ae2.item.MEIntelligentCellItem
import com.rcdiamond.aifactory.shared.SharedSessionLimits
import com.rcdiamond.aifactory.shared.SharedSessionSavedData
import com.rcdiamond.aifactory.shared.SharedSessionStore
import java.util.UUID
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent

/** Display-only NBT summary, synchronized through normal inventory packets, never used for quota or session lookup. */
internal object MEIntelligentCellTooltip {
    private const val TAG_SUMMARY = "ai_factory:cell_summary"
    private const val TAG_TITLES = "titles"
    private const val TAG_USED_BYTES = "usedBytes"

    fun register() {
        MinecraftForge.EVENT_BUS.addListener<TickEvent.PlayerTickEvent> { event ->
            if (event.phase == TickEvent.Phase.END) {
                val player = event.player as? ServerPlayer ?: return@addListener
                val store = SharedSessionSavedData.get(player.server).store
                refreshMenu(player.inventoryMenu, store)
                if (player.containerMenu !== player.inventoryMenu) {
                    refreshMenu(player.containerMenu, store)
                }
            }
        }
    }

    private fun refreshMenu(menu: AbstractContainerMenu, store: SharedSessionStore) {
        for (index in menu.slots.indices) {
            val stack = menu.slots[index].item
            if (stack.item is MEIntelligentCellItem) refresh(stack, store)
        }
        val carried = menu.carried
        if (carried.item is MEIntelligentCellItem) refresh(carried, store)
    }

    private fun refresh(stack: ItemStack, store: SharedSessionStore) {
        val tag = stack.tag ?: return
        refresh(tag, MEIntelligentCellRef.read(stack), store)
    }

    /** Only called on the server thread; unchanged summaries retain their tag and cause no slot update. */
    fun refresh(tag: CompoundTag, cellId: UUID?, store: SharedSessionStore) {
        if (cellId == null) {
            tag.remove(TAG_SUMMARY)
            return
        }
        val titles = store.titles(cellId)
        val usedBytes = store.usedBytes(cellId)
        val summary = tag.get(TAG_SUMMARY) as? CompoundTag
        val previousTitles = summary?.get(TAG_TITLES) as? ListTag
        if (summary != null && summary.contains(TAG_USED_BYTES, Tag.TAG_LONG.toInt()) &&
            summary.getLong(TAG_USED_BYTES) == usedBytes && previousTitles != null &&
            previousTitles.size == titles.size
        ) {
            var matches = true
            for (index in titles.indices) {
                if (previousTitles.getString(index) != titles[index]) {
                    matches = false
                    break
                }
            }
            if (matches) return
        }
        val updated = CompoundTag()
        val titleTags = ListTag()
        for (index in titles.indices) titleTags.add(StringTag.valueOf(titles[index]))
        updated.put(TAG_TITLES, titleTags)
        updated.putLong(TAG_USED_BYTES, usedBytes)
        tag.put(TAG_SUMMARY, updated)
    }

    fun append(tag: CompoundTag?, capacityBytes: Long, tooltip: MutableList<Component>) {
        if (tag?.hasUUID(MEIntelligentCellRef.TAG_CELL_ID) == true) {
            tooltip.add(Component.translatable(
                "tooltip.ai_factory.me_intelligent_cell.id",
                tag.getUUID(MEIntelligentCellRef.TAG_CELL_ID).toString().take(8),
            ).withStyle(ChatFormatting.GRAY))
        }
        val summary = tag?.get(TAG_SUMMARY) as? CompoundTag
        val titles = summary?.get(TAG_TITLES) as? ListTag
        if (titles != null) {
            for (index in 0 until titles.size) tooltip.add(Component.literal(titles.getString(index)))
        }
        tooltip.add(Component.translatable(
            "tooltip.ai_factory.me_intelligent_cell.capacity",
            summary?.getLong(TAG_USED_BYTES) ?: 0L,
            capacityBytes,
        ))
        tooltip.add(Component.translatable(
            "tooltip.ai_factory.me_intelligent_cell.sessions",
            titles?.size ?: 0,
            SharedSessionLimits.MAX_SESSIONS_PER_CELL,
        ))
    }
}
