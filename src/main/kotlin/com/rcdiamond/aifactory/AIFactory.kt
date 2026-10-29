package com.rcdiamond.aifactory

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.network.GameActionNetwork
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(AIFactory.MODID)
object AIFactory {
    const val MODID = "ai_factory"
    private val logger = LogUtils.getLogger()

    init {
        // The class name is deliberately a string: no AE2 type enters the base mod's linkage.
        if (ModList.get().isLoaded("ae2")) {
            Class.forName("com.rcdiamond.aifactory.addon.ae2.Ae2Addon")
                .getMethod("initialize").invoke(null)
        }
        if (ModList.get().isLoaded("integrateddynamics")) {
            Class.forName("com.rcdiamond.aifactory.addon.integrated_dynamics.IntegratedDynamicsAddon")
                .getMethod("initialize").invoke(null)
        }
        MOD_BUS.addListener<FMLCommonSetupEvent> {
            it.enqueueWork {
                GameActionNetwork.register()
                SharedSessionNetwork.register()
            }
            logger.info("AI-Factory initialized")
        }
    }
}
