package com.rcdiamond.aifactory.addon.integrated_dynamics.client

import com.rcdiamond.aifactory.addon.integrated_dynamics.client.tool.IntegratedDynamicsToolGateway
import com.rcdiamond.aifactory.addon.integrated_dynamics.client.tool.IntegratedDynamicsTools
import com.rcdiamond.aifactory.client.shared.SharedAgentProfile
import com.rcdiamond.aifactory.client.shared.SharedAgentSessionRuntime

/** Independent context, leases, tools and turns; this composition root has no AE2 dependency. */
object IntegratedDynamicsAgentRuntime : SharedAgentSessionRuntime(
    SharedAgentProfile(
        operationPrefix = "id_",
        ownerField = "terminalId",
        usesCells = false,
        browserTitleKey = "ai_factory.id.sessions.title",
        chatTitleKey = "ai_factory.id.chat.title",
        systemInstruction = "\nYou are the Integrated Dynamics network assistant for this terminal. " +
            "Inspect the actual network, parts and variables first. Use id_compile to preview the typed " +
            "Haskell-like DSL and id_make_cards only when the user requests creating cards. Programs use " +
            "input name : Type; let name = expression; output port = expression; with explicit semicolons. " +
            "Bindings use existing variable IDs or reader aspect IDs reported by the network; never invent " +
            "IDs, types or operator signatures. Correct errors using their phase, code, line, column and hint. " +
            "Card creation consumes blank ID cards and puts all definitions in the player's main inventory. " +
            "Explain that dependency cards must be placed in the same ID network's Variable Store and output " +
            "cards installed manually. Do not claim preview deployed anything, automation ran, or logistics " +
            "were changed. No live variable evaluation, AE2, command, item-grant or JEI tools are available.",
    ),
    { manager, gateway ->
        IntegratedDynamicsTools.register(manager, IntegratedDynamicsToolGateway(gateway))
    },
)
