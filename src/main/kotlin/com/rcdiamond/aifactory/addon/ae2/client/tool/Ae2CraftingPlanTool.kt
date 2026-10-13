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
 * Advertises and validates one `ae2_crafting_plan` call.
 *
 * This asks the server to compute a plan, never to craft. The plan describes what the network would
 * need at the moment it was computed, so the model must treat the numbers as a snapshot instead of a
 * reservation.
 */
internal class Ae2CraftingPlanTool(
    private val gateway: suspend (String, JsonObject) -> JsonObject,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = Ae2ToolLimits.OPERATION_CRAFTING_PLAN,
        description = "Computes a real Applied Energistics crafting plan for one key from " +
            "${Ae2ToolLimits.OPERATION_READ_INVENTORY}. It answers what it would take to craft the requested " +
            "amount: the actual missing items, the crafting storage in bytes the job would need and whether the " +
            "result is only a simulation because the network cannot craft it all. Nothing is submitted or " +
            "reserved: no crafting job starts and no items are extracted. The calculation runs on the server and " +
            "can take several seconds. Amounts and results describe the network as it was when the plan was " +
            "computed, so read the inventory again if you need current counts. This tool cannot craft anything " +
            "by itself.",
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
                    put("description", "How many to plan for, at least 1. Defaults to 1.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(Ae2ToolArguments.KEY))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = Ae2Tools.parameters(Ae2ToolLimits.OPERATION_CRAFTING_PLAN, arguments)
        val resolved = validated { Ae2ToolArguments.craftingPlan(parameters) }
        return gateway(Ae2ToolLimits.OPERATION_CRAFTING_PLAN, resolved.toJson())
    }
}
