package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolArguments
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement

/**
 * Advertises and validates one `id_list_parts` call and forwards the bound window to the session.
 *
 * Only the filter and the page window are model supplied; the terminal, its network and the player
 * are resolved by the server from the session that carries the request.
 */
internal class IdListPartsTool(
    private val gateway: IntegratedDynamicsToolGateway,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS,
        description = "Lists the parts of the terminal's Integrated Dynamics network, one bounded page " +
            "at a time, without changing anything. Each entry reports the part's numeric network id, its " +
            "part type, its translation key, its network channel, the cable position it sits at, and the " +
            "target block it points at, each as dimension, x, y, z and a face where the side exists. " +
            "Entries are sorted by id and type. Pagination is live: network changes may shift entries " +
            "between calls. 'total' is the match count; continue while 'has_more' is true.",
        parameters = listingSchema(
            "Case insensitive text matched against a part's type id and its translation key. " +
                "Omit it to list every part.",
        ),
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = IntegratedDynamicsTools.parameters(IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS, arguments)
        val resolved = validated { IntegratedDynamicsToolArguments.listParts(parameters) }
        return gateway.execute(IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS, resolved.toJson())
    }
}
