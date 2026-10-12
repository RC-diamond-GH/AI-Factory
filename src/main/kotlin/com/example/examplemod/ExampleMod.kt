package com.example.examplemod

import com.mojang.logging.LogUtils
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.registerConfig

@Mod(ExampleMod.MODID)
object ExampleMod {
    const val MODID = "examplemod"
    private val logger = LogUtils.getLogger()

    private val blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID)
    private val items = DeferredRegister.create(ForgeRegistries.ITEMS, MODID)
    private val creativeModeTabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)

    val EXAMPLE_BLOCK = blocks.register("example_block") {
        Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f))
    }
    val EXAMPLE_BLOCK_ITEM = items.register("example_block") {
        BlockItem(EXAMPLE_BLOCK.get(), Item.Properties())
    }
    val EXAMPLE_ITEM = items.register("example_item") {
        Item(Item.Properties().food(FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2.0f).build()))
    }
    val EXAMPLE_TAB = creativeModeTabs.register("example_tab") {
        CreativeModeTab.builder()
            .title(Component.translatable("creativetab.examplemod.example_tab"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon { EXAMPLE_BLOCK_ITEM.get().defaultInstance }
            .displayItems { _, output ->
                output.accept(EXAMPLE_BLOCK_ITEM.get())
                output.accept(EXAMPLE_ITEM.get())
            }
            .build()
    }

    init {
        val modEventBus = MOD_BUS
        blocks.register(modEventBus)
        items.register(modEventBus)
        creativeModeTabs.register(modEventBus)
        modEventBus.addListener<FMLCommonSetupEvent>(::commonSetup)
        modEventBus.addListener<BuildCreativeModeTabContentsEvent>(::addCreative)
        modEventBus.register(Config)
        MinecraftForge.EVENT_BUS.register(this)
        registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            logger.info("HELLO FROM COMMON SETUP (Kotlin)")
            if (Config.logDirtBlock) {
                logger.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT))
            }
            logger.info("{}{}", Config.magicNumberIntroduction, Config.magicNumber)
            Config.items.forEach { logger.info("ITEM >> {}", it) }
        }
    }

    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM)
        }
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        logger.info("HELLO from server starting (Kotlin)")
    }
}
