package com.example.examplemod

import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientModEvents {
    private val logger = LogUtils.getLogger()

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            logger.info("HELLO FROM CLIENT SETUP (Kotlin)")
            logger.info("MINECRAFT NAME >> {}", Minecraft.getInstance().user.name)
        }
    }
}
