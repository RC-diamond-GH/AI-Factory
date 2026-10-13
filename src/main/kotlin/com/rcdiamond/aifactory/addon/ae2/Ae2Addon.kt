package com.rcdiamond.aifactory.addon.ae2

import appeng.api.storage.StorageCells
import appeng.items.parts.PartItem
import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.addon.ae2.item.MEIntelligentCellItem
import com.rcdiamond.aifactory.addon.ae2.item.MEIntelligentCellTier
import com.rcdiamond.aifactory.addon.ae2.part.MEIntelligentTerminalPart
import com.rcdiamond.aifactory.addon.ae2.server.Ae2CellBackend
import com.rcdiamond.aifactory.addon.ae2.server.Ae2ToolExecutor
import com.rcdiamond.aifactory.addon.ae2.storage.MEIntelligentCellHandler
import com.rcdiamond.aifactory.addon.ae2.storage.MEIntelligentCellTooltip
import com.rcdiamond.aifactory.shared.SharedSessionService
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
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
 * - The content: one terminal *part item* (the terminal itself is an AE2 cable part, so it has no block and
 *   no block entity of its own - AE2 installs, saves and removes it with the cable it sits on), the five
 *   intelligent cell items, and one creative tab. The registries are handed to the mod event bus here,
 *   during mod construction, which is the only moment Forge accepts them.
 * - The terminal's screen models: also here, because AE2 bakes the part model registry during client model
 *   loading and freezes it - a later call (common setup, for instance) would fail.
 * - The behaviour: during `FMLCommonSetupEvent`, i.e. after AE2 has finished its own setup, this installs
 *   the cell handler, registers the tool executor's tick hook, and finally publishes [Ae2CellBackend] as the
 *   shared session service's backend. Until that last step every shared-session request fails closed with
 *   "not available on this server".
 *
 * ### Registry ids (used by the resource files of the base mod)
 * - item `ai_factory:me_intelligent_terminal`
 * - items `ai_factory:me_intelligent_cell_1k|4k|16k|64k|256k`
 * - creative tab `ai_factory:intelligent_storage`
 */
object Ae2Addon {
    private val LOGGER = LogUtils.getLogger()

    private val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AIFactory.MODID)
    private val CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AIFactory.MODID)

    /** Registry path of the terminal item. */
    private const val TERMINAL_NAME = "me_intelligent_terminal"

    /**
     * The terminal item.
     *
     * It is an AE2 [PartItem], so using it on a cable places [MEIntelligentTerminalPart] on the clicked
     * face; the item id, and with it the recipe and the creative tab entry, is unchanged.
     */
    val TERMINAL_ITEM: RegistryObject<PartItem<MEIntelligentTerminalPart>> =
        ITEMS.register<PartItem<MEIntelligentTerminalPart>>(TERMINAL_NAME) {
            PartItem(Item.Properties(), MEIntelligentTerminalPart::class.java) { partItem ->
                MEIntelligentTerminalPart(partItem)
            }
        }

    /** One item per tier, keyed by the tier so callers never repeat a registry path. */
    val CELL_ITEMS: Map<MEIntelligentCellTier, RegistryObject<Item>> =
        MEIntelligentCellTier.entries.associateWith { tier ->
            ITEMS.register<Item>(tier.registryName) { MEIntelligentCellItem(tier) }
        }

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

    /**
     * Called reflectively by the base mod's entry point, once, when AE2 is present.
     *
     * Registration happens here, during mod construction: the Forge registries are filled during Forge's
     * registry events, while [MEIntelligentTerminalPart.registerModels] has to run before AE2 freezes its
     * part model registry on the client. The AE2-dependent wiring waits for common setup, after AE2 itself
     * has installed its cell handlers.
     */
    @JvmStatic
    fun initialize() {
        ITEMS.register(MOD_BUS)
        CREATIVE_TABS.register(MOD_BUS)
        MEIntelligentTerminalPart.registerModels()

        MOD_BUS.addListener<FMLCommonSetupEvent> { event ->
            event.enqueueWork { bootstrap() }
        }
    }

    private fun bootstrap() {
        // Makes the five items mountable storage cells of a normal ME Drive. Registered here, after AE2's own
        // handlers, and once: registering the same instance twice is rejected by AE2.
        StorageCells.addCellHandler(MEIntelligentCellHandler)

        // The tool executor owns the tick hook that finishes asynchronous crafting planning.
        Ae2ToolExecutor.register()
        MEIntelligentCellTooltip.register()

        // Last, so a request that arrives while we are still setting up fails closed instead of half working.
        SharedSessionService.backend = Ae2CellBackend

        LOGGER.info(
            "AI-Factory AE2 addon ready: intelligent terminal part and {} intelligent cell tiers",
            MEIntelligentCellTier.entries.size,
        )
    }
}
