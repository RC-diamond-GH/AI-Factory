package com.rcdiamond.aifactory.addon.ae2

import appeng.api.storage.StorageCells
import appeng.blockentity.AEBaseBlockEntity
import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.shared.SharedSessionService
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * Entry point of the optional AE2 addon.
 *
 * The base mod never links against AE2: it only loads the class name of this object as a string and calls
 * its static [initialize] when `ModList.isLoaded("ae2")`. Everything AE2-typed therefore lives below this
 * object, and a server without AE2 simply never loads any of it.
 *
 * ### What is registered, and when
 * - The content: one terminal block with its block entity type and block item, the five intelligent cell
 *   items, and one creative tab. The registries are handed to the mod event bus here, during mod
 *   construction, which is the only moment Forge accepts them.
 * - The behaviour: during `FMLCommonSetupEvent`, i.e. after AE2 has finished its own setup, this installs
 *   the cell handler, attaches the registered block entity type to the block, teaches AE2 which item
 *   represents the block entity (so the grid node has a visual representation), registers the tool
 *   executor's tick hook, and finally publishes [Ae2CellBackend] as the shared session service's backend.
 *   Until that last step every shared-session request fails closed with "not available on this server".
 *
 * ### Registry ids (used by the resource files of the base mod)
 * - block/item `ai_factory:me_intelligent_terminal`
 * - items `ai_factory:me_intelligent_cell_1k|4k|16k|64k|256k`
 * - creative tab `ai_factory:intelligent_storage`
 */
object Ae2Addon {
    private val LOGGER = LogUtils.getLogger()

    private val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AIFactory.MODID)
    private val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AIFactory.MODID)
    private val BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AIFactory.MODID)
    private val CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AIFactory.MODID)

    /** Registry path shared by the terminal block, its block item and its block entity. */
    private const val TERMINAL_NAME = "me_intelligent_terminal"

    private val terminalBlock = MEIntelligentTerminalBlock()

    val TERMINAL_BLOCK: RegistryObject<MEIntelligentTerminalBlock> =
        BLOCKS.register(TERMINAL_NAME) { terminalBlock }

    val TERMINAL_ITEM: RegistryObject<Item> =
        ITEMS.register<Item>(TERMINAL_NAME) { BlockItem(terminalBlock, Item.Properties()) }

    /** One item per tier, keyed by the tier so callers never repeat a registry path. */
    val CELL_ITEMS: Map<MEIntelligentCellTier, RegistryObject<Item>> =
        MEIntelligentCellTier.entries.associateWith { tier ->
            ITEMS.register<Item>(tier.registryName) { MEIntelligentCellItem(tier) }
        }

    /**
     * The terminal's block entity type.
     *
     * Assigned in the init block because the type's factory needs the type itself (the block entity
     * constructor takes it, as every Forge block entity does); the lambda only runs when a block entity is
     * created, long after registration, so the read is always of the finished value.
     */
    lateinit var TERMINAL_BLOCK_ENTITY: RegistryObject<BlockEntityType<MEIntelligentTerminalBlockEntity>>
        private set

    /** The tab the addon's content shows up in; it exists only while AE2 is loaded. */
    val CREATIVE_TAB: RegistryObject<CreativeModeTab> =
        CREATIVE_TABS.register("intelligent_storage") {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ai_factory.intelligent_storage"))
                .icon { ItemStack(CELL_ITEMS.getValue(MEIntelligentCellTier.K1).get()) }
                .displayItems { _, output ->
                    for (tier in MEIntelligentCellTier.entries) {
                        output.accept(CELL_ITEMS.getValue(tier).get())
                    }
                    output.accept(TERMINAL_ITEM.get())
                }
                .build()
        }

    init {
        TERMINAL_BLOCK_ENTITY = BLOCK_ENTITIES.register(TERMINAL_NAME) {
            val supplier =
                BlockEntityType.BlockEntitySupplier<MEIntelligentTerminalBlockEntity> { pos, state ->
                    MEIntelligentTerminalBlockEntity(TERMINAL_BLOCK_ENTITY.get(), pos, state)
                }
            BlockEntityType.Builder.of(supplier, terminalBlock).build(null)
        }
    }

    /**
     * Called reflectively by the base mod's entry point, once, when AE2 is present.
     *
     * Only registration happens here: the registries are filled during Forge's registry events, and the
     * AE2-dependent wiring waits for common setup, after AE2 itself has set up its cell handlers.
     */
    @JvmStatic
    fun initialize() {
        BLOCKS.register(MOD_BUS)
        ITEMS.register(MOD_BUS)
        BLOCK_ENTITIES.register(MOD_BUS)
        CREATIVE_TABS.register(MOD_BUS)

        MOD_BUS.addListener<FMLCommonSetupEvent> { event ->
            event.enqueueWork { bootstrap() }
        }
    }

    private fun bootstrap() {
        val blockEntityType = TERMINAL_BLOCK_ENTITY.get()

        // The registry entry exists now, so the block can create and find its block entities.
        terminalBlock.setBlockEntity(MEIntelligentTerminalBlockEntity::class.java, blockEntityType, null, null)

        // Tells AE2 which item stands for this block entity; a grid node shows it as its visual
        // representation, which is what network tools and the grid debug view use.
        AEBaseBlockEntity.registerBlockEntityItem(blockEntityType, TERMINAL_ITEM.get())

        // Makes the five items mountable storage cells of a normal ME Drive. Registered here, after AE2's own
        // handlers, and once: registering the same instance twice is rejected by AE2.
        StorageCells.addCellHandler(MEIntelligentCellHandler)

        // The tool executor owns the tick hook that finishes asynchronous crafting planning.
        Ae2ToolExecutor.register()

        // Last, so a request that arrives while we are still setting up fails closed instead of half working.
        SharedSessionService.backend = Ae2CellBackend

        LOGGER.info(
            "AI-Factory AE2 addon ready: intelligent terminal and {} intelligent cell tiers",
            MEIntelligentCellTier.entries.size,
        )
    }
}
