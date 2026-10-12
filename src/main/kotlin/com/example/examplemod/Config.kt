package com.example.examplemod

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.registries.ForgeRegistries

object Config {
    private val builder = ForgeConfigSpec.Builder()
    private val logDirtBlockValue = builder
        .comment("Whether to log the dirt block on common setup")
        .define("logDirtBlock", true)
    private val magicNumberValue = builder
        .comment("A magic number")
        .defineInRange("magicNumber", 42, 0, Int.MAX_VALUE)
    private val magicNumberIntroductionValue = builder
        .comment("What you want the introduction message to be for the magic number")
        .define("magicNumberIntroduction", "The magic number is... ")
    private val itemStrings = builder
        .comment("A list of items to log on common setup.")
        .defineListAllowEmpty("items", listOf("minecraft:iron_ingot"), ::validateItemName)

    val SPEC: ForgeConfigSpec = builder.build()

    var logDirtBlock = true
        private set
    var magicNumber = 42
        private set
    var magicNumberIntroduction = "The magic number is... "
        private set
    var items: Set<Item> = emptySet()
        private set

    private fun validateItemName(value: Any): Boolean {
        if (value !is String) return false
        val id = ResourceLocation.tryParse(value) ?: return false
        return ForgeRegistries.ITEMS.containsKey(id)
    }

    @SubscribeEvent
    fun onLoad(event: ModConfigEvent) {
        if (event.config.getSpec() !== SPEC) return
        logDirtBlock = logDirtBlockValue.get()
        magicNumber = magicNumberValue.get()
        magicNumberIntroduction = magicNumberIntroductionValue.get()
        items = itemStrings.get().mapTo(linkedSetOf()) { name ->
            requireNotNull(ForgeRegistries.ITEMS.getValue(ResourceLocation(name))) {
                "Configured item is not registered: $name"
            }
        }
    }
}
