package com.rcdiamond.aifactory.addon.ae2.client.tool

import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolArguments
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `ae2_request_crafting` call.
 *
 * Unlike `ae2_crafting_plan` this has a real side effect: the server computes the plan and then submits
 * it to a crafting CPU, which extracts the used items from the network. The schema matches the plan tool
 * [Ae2ToolArguments.craftingPlan], so the two stay in step and the model cannot smuggle a request past the
 * client validation the plan tool enforces.
 */
internal class Ae2RequestCraftingTool(
    private val gateway: suspend (String, JsonObject) -> JsonObject,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = Ae2ToolLimits.OPERATION_REQUEST_CRAFTING,
        description = "IRREVERSIBLE: computes a real Applied Energistics crafting plan for one item key from " +
            "${Ae2ToolLimits.OPERATION_READ_INVENTORY} and then submits it, so a crafting CPU starts a real " +
            "job. The moment the job is accepted AE2 extracts the ingredients from the network; the crafted " +
            "items go back into the network, not into the player's inventory, and canceling later does not undo " +
            "that extraction. Call this only when the user clearly asked to craft or request that item - never " +
            "on your own initiative, and never merely to check something: use " +
            "${Ae2ToolLimits.OPERATION_CRAFTING_PLAN} to preview the cost without crafting. Only item keys the " +
            "network can already craft are accepted, and a plan that is short of ingredients is rejected with " +
            "the real missing items and submits nothing. The calculation runs on the server and can take " +
            "several seconds; if the call times out or errors, do not retry automatically - report it and let " +
            "the user decide, because a retry could submit a second job. A successful answer carries " +
            "'submitted': true and describes the network's new job, not the player's inventory.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(Ae2ToolArguments.KEY) {
                    put("type", "string")
                    put("minLength", Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
                    put("maxLength", Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
                    put("pattern", "^[0-9a-f]{${Ae2ToolLimits.KEY_FINGERPRINT_LENGTH}}$")
                    put("description", "The 'key' exactly as returned by ${Ae2ToolLimits.OPERATION_READ_INVENTORY}.")
                }
                putJsonObject(Ae2ToolArguments.AMOUNT) {
                    put("type", "integer")
                    put("minimum", Ae2ToolLimits.MIN_AMOUNT)
                    put("maximum", Ae2ToolLimits.MAX_AMOUNT)
                    put("description", "How many to craft, at least 1. Defaults to 1.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(Ae2ToolArguments.KEY))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = Ae2Tools.parameters(Ae2ToolLimits.OPERATION_REQUEST_CRAFTING, arguments)
        val resolved = validated { Ae2ToolArguments.craftingPlan(parameters) }
        return gateway(Ae2ToolLimits.OPERATION_REQUEST_CRAFTING, resolved.toJson())
    }
}
