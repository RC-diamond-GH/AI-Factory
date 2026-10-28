package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.block.IntegratedDynamicsTerminalBlock
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.block.IntegratedDynamicsTerminalBlockEntity
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server.IntegratedDynamicsSessions
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * Entry point of the optional Integrated Dynamics addon.
 *
 * The base mod never links against Integrated Dynamics: it only loads this class's name as a string and calls
 * its static [initialize] when `ModList.isLoaded("integrateddynamics")`. Everything Integrated Dynamics-typed
 * therefore lives below this object, and a server without Integrated Dynamics never loads any of it.
 *
 * ### What is registered
 * - `block ai_factory:integrated_dynamics_terminal` and `item ai_factory:integrated_dynamics_terminal`
 *   ([IntegratedDynamicsTerminalBlock] plus its block item, which is what carries the terminal identity and
 *   session references in its `BlockEntityTag` when the block is broken).
 * - `block_entity_type ai_factory:integrated_dynamics_terminal` ([IntegratedDynamicsTerminalBlockEntity],
 *   which is the block's Integrated Dynamics network element provider).
 * - `creative_mode_tab ai_factory:integrated_dynamics`, so the terminal is reachable in creative mode; the
 *   recipe makes it reachable in survival.
 *
 * ### What is wired
 * The shared session transport gets an independent `id_` endpoint whose operations are handled by
 * [IntegratedDynamicsSessions]; its leases are cleaned up by [IntegratedDynamicsSessions.install]. This addon
 * shares the bounded JSON transport with the optional AE2 addon but nothing else - no store, no leases, no
 * cells. Until this method runs, any `id_` request is answered by the transport as an unknown operation rather
 * than half-working.
 */
object IntegratedDynamicsAddon {
    private val LOGGER = LogUtils.getLogger()

    /** Registry path shared by the block, its block item and its block entity type. */
    private const val TERMINAL_NAME = "integrated_dynamics_terminal"

    private val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AIFactory.MODID)
    private val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AIFactory.MODID)
    private val BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AIFactory.MODID)
    private val CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AIFactory.MODID)

    /** The terminal block; cable-connected and part-owned by its own block entity. */
    val TERMINAL_BLOCK: RegistryObject<IntegratedDynamicsTerminalBlock> =
        BLOCKS.register(TERMINAL_NAME) { IntegratedDynamicsTerminalBlock() }

    /**
     * The block entity type. Created through the plain Forge registry rather than Integrated Dynamics'
     * config machinery, but with the same shape Integrated Dynamics itself uses: one supplier and the block
     * set. Blocks are registered before block entity types, so [TERMINAL_BLOCK] is resolved here.
     */
    val TERMINAL_BLOCK_ENTITY: RegistryObject<BlockEntityType<IntegratedDynamicsTerminalBlockEntity>> =
        BLOCK_ENTITIES.register(TERMINAL_NAME) {
            BlockEntityType.Builder.of(
                BlockEntityType.BlockEntitySupplier<IntegratedDynamicsTerminalBlockEntity> { pos, state ->
                    IntegratedDynamicsTerminalBlockEntity(pos, state)
                },
                TERMINAL_BLOCK.get(),
            ).build(null)
        }

    /** The placeable terminal; a plain vanilla block item, so `BlockEntityTag` is applied on placement. */
    val TERMINAL_ITEM: RegistryObject<Item> =
        ITEMS.register(TERMINAL_NAME) { BlockItem(TERMINAL_BLOCK.get(), Item.Properties()) }

    /** The tab the terminal shows up in; it exists only while Integrated Dynamics is loaded. */
    val CREATIVE_TAB: RegistryObject<CreativeModeTab> =
        CREATIVE_TABS.register("integrated_dynamics") {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ai_factory.integrated_dynamics"))
                .icon { ItemStack(TERMINAL_ITEM.get()) }
                .displayItems { _, output -> output.accept(TERMINAL_ITEM.get()) }
                .build()
        }

    /**
     * Called reflectively by the base mod's entry point, once, when Integrated Dynamics is present.
     *
     * The content registries are handed to the mod event bus here, during mod construction, which is the only
     * moment Forge accepts them. The shared session endpoint and this addon's lease cleanup are installed
     * here as well: they depend on nothing Integrated Dynamics has to set up first, and installing them now
     * means a request that arrives during startup is handled instead of racing the registration.
     */
    @JvmStatic
    fun initialize() {
        BLOCKS.register(MOD_BUS)
        ITEMS.register(MOD_BUS)
        BLOCK_ENTITIES.register(MOD_BUS)
        CREATIVE_TABS.register(MOD_BUS)

        SharedSessionNetwork.registerEndpoint("id_", IntegratedDynamicsSessions::handle)
        IntegratedDynamicsSessions.install()

        LOGGER.info("AI-Factory Integrated Dynamics addon ready: integrated dynamics terminal")
    }
}
