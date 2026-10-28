package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool.IntegratedDynamicsToolGateway
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool.IntegratedDynamicsTools
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
            "Use only the exposed read-only network inspection tools. Report actual network, part and " +
            "variable metadata; do not claim to have created variable cards, compiled DSL, changed " +
            "logistics, evaluated variables or executed automation. AE2 and command tools are not available.",
    ),
    { manager, gateway ->
        IntegratedDynamicsTools.register(manager, IntegratedDynamicsToolGateway(gateway))
    },
)
