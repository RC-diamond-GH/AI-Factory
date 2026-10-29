package com.rcdiamond.aifactory.addon.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.integrated_dynamics.tool.IntegratedDynamicsToolArguments
import com.rcdiamond.aifactory.addon.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement

/**
 * Advertises and validates one `id_list_variables` call and forwards the bound window to the session.
 *
 * Only the filter and the page window are model supplied; the terminal, its network and the player
 * are resolved by the server from the session that carries the request.
 */
internal class IdListVariablesTool(
    private val gateway: IntegratedDynamicsToolGateway,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES,
        description = "Lists resolvable variable definitions in the terminal's network, including " +
            "Variable Stores and active part card slots, one bounded page at a time. Each entry reports " +
            "id, optional label, output type where known, kind, and native operator/aspect/proxy references. " +
            "The 'valid' field is only the facade's basic validity flag, not dependency/type validation " +
            "or successful runtime evaluation. Constant contents and live values are not read. Entries " +
            "are sorted by id; pagination is live and changes may shift entries between calls. " +
            "'total' is the match count; continue while 'has_more' is true.",
        parameters = listingSchema(
            "Case insensitive text matched against a variable's id, label, output type and its referenced " +
                "operator or aspect ids. Omit it to list every variable.",
        ),
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters =
            IntegratedDynamicsTools.parameters(IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES, arguments)
        val resolved = validated { IntegratedDynamicsToolArguments.listVariables(parameters) }
        return gateway.execute(IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES, resolved.toJson())
    }
}
