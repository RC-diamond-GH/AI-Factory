package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.addon.ae2.client.tool.Ae2Tools
import com.rcdiamond.aifactory.addon.jei.client.tool.JeiTools
import net.minecraftforge.fml.ModList

/** ME sessions keep their own runtime and mounted-cell protocol. */
object ClientSharedSessionRuntime : SharedAgentSessionRuntime(
    SharedAgentProfile("", "cellId", true, "ai_factory.shared.title", "ai_factory.shared.chat_title"),
    { manager, gateway ->
        if (ModList.get().isLoaded("ae2")) Ae2Tools.register(manager, gateway)
        if (ModList.get().isLoaded("jei")) JeiTools.register(manager)
    },
)
