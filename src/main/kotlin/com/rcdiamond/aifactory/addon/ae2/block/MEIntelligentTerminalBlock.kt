package com.rcdiamond.aifactory.addon.ae2.block

import appeng.block.AEBaseBlock
import appeng.block.AEBaseEntityBlock
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.addon.ae2.Ae2Addon
import com.rcdiamond.aifactory.addon.ae2.blockentity.MEIntelligentTerminalBlockEntity
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.phys.BlockHitResult

/**
 * The intelligent terminal block.
 *
 * Using it (without sneaking, and without a memory card in hand, which the AE2 base class handles) validates
 * the terminal server-side through [MEIntelligentTerminalBlockEntity.resolve] and then asks the core to send
 * the client the terminal locator: no container menu is opened, because the session list is a screen the
 * client opens on its own. A rejected use reports the safe reason on the action bar and never opens
 * anything - the server is authoritative, the client only renders.
 *
 * The block has no orientation property (the AE2 base class adds none by default), so its blockstate is a
 * single variant with a fixed model orientation.
 *
 * ## Drops without AE2 installed
 * This block only exists while AE2 is loaded, so an ordinary `minecraft:item` loot entry naming it could not
 * be resolved by a datapack that parses on a server without AE2. The terminal therefore supplies its own
 * drop: the loot table uses a `minecraft:dynamic` entry named [DYNAMIC_DROP], and [getDrops] registers a
 * supplier for that name. Dynamic entries are never resolved against the item registry at parse time, so the
 * resource stays valid in every configuration while the block still drops its real item, with the settings
 * export the AE2 base class performs.
 */
class MEIntelligentTerminalBlock : AEBaseEntityBlock<MEIntelligentTerminalBlockEntity>(AEBaseBlock.metalProps()) {

    init {
        // The block entity class is enough for lookups and drops; the registered type is attached during
        // the addon's common setup, once the registry entries exist.
        setBlockEntity(MEIntelligentTerminalBlockEntity::class.java, null, null, null)
    }

    override fun onActivated(
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        heldItem: ItemStack?,
        hit: BlockHitResult,
    ): InteractionResult {
        if (player.isShiftKeyDown) {
            return InteractionResult.PASS
        }
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.SUCCESS
        val serverLevel = level as? ServerLevel ?: return InteractionResult.SUCCESS
        val terminal = getBlockEntity(level, pos) ?: return InteractionResult.PASS

        try {
            terminal.resolve(serverPlayer, allowRemote = false)
        } catch (rejected: IllegalArgumentException) {
            serverPlayer.displayClientMessage(Component.literal(rejected.message ?: DEFAULT_REJECTION), true)
            return InteractionResult.SUCCESS
        }

        SharedSessionNetwork.openTerminal(serverPlayer, locator(serverLevel, pos))
        return InteractionResult.SUCCESS
    }

    /**
     * Registers the terminal's own drop before the loot table is evaluated, so the table can stay free of an
     * item id that only exists while the addon is loaded.
     */
    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        builder.withDynamicDrop(DYNAMIC_DROP) { consumer ->
            consumer.accept(ItemStack(Ae2Addon.TERMINAL_ITEM.get()))
        }
        return super.getDrops(state, builder)
    }

    private fun locator(level: ServerLevel, pos: BlockPos): JsonObject = buildJsonObject {
        put("dimension", level.dimension().location().toString())
        put("x", pos.x)
        put("y", pos.y)
        put("z", pos.z)
    }

    companion object {
        /** Name of the dynamic loot entry the terminal fills in [getDrops]. */
        val DYNAMIC_DROP: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(AIFactory.MODID, "me_intelligent_terminal")

        private const val DEFAULT_REJECTION = "That intelligent terminal cannot be used right now"
    }
}
