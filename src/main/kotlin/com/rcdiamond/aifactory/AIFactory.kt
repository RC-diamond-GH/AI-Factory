package com.rcdiamond.aifactory

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.network.GameActionNetwork
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(AIFactory.MODID)
object AIFactory {
    const val MODID = "ai_factory"
    private val logger = LogUtils.getLogger()

    init {
        MOD_BUS.addListener<FMLCommonSetupEvent> {
            it.enqueueWork { GameActionNetwork.register() }
            logger.info("AI-Factory initialized")
        }
    }
}
