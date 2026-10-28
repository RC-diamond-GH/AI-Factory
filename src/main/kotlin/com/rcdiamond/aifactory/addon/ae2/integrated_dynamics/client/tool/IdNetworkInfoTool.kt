package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolArguments
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `id_network_info` call.
 *
 * The terminal, its network and the player are resolved by the server from the session that carries
 * the request, so this tool takes no arguments at all and refuses any the model invents.
 */
internal class IdNetworkInfoTool(
    private val gateway: IntegratedDynamicsToolGateway,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO,
        description = "Summarizes the Integrated Dynamics network the terminal is attached to, changing " +
            "nothing: native cable, element and part counts, unique resolvable variable definitions in " +
            "loaded containers, energy consumption, and exposed ingredient components (item, fluid, energy). " +
            "It also reports initialization and killed state. Use it first to judge a network's size before " +
            "listing parts or variables, or to confirm the terminal is attached to a usable network. " +
            "Takes no arguments.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {}
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = IntegratedDynamicsTools.parameters(IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO, arguments)
        val resolved = validated { IntegratedDynamicsToolArguments.networkInfo(parameters) }
        return gateway.execute(IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO, resolved)
    }
}
