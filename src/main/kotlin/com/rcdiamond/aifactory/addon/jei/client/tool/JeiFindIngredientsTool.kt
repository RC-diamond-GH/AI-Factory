package com.rcdiamond.aifactory.addon.jei.client.tool

import com.rcdiamond.aifactory.addon.jei.client.JeiQueryService
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `jei_find_ingredients` call.
 *
 * The search only returns display candidates. Their keys are runtime-scoped, so the description tells
 * the model to feed a key back into [JeiQueryRecipesTool] during the same JEI session; a plain filter
 * does not change JEI's own search box, and every request is handed to [gateway] for the current JEI
 * runtime to answer.
 */
internal class JeiFindIngredientsTool(
    private val gateway: JeiQueryService,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = JeiToolLimits.OPERATION_FIND_INGREDIENTS,
        description = "Searches JEI's registered items, fluids and custom ingredient types and returns one bounded " +
            "page of visible candidates. Use it when you do not already know an exact resource id, then pass a " +
            "candidate's 'key' to ${JeiToolLimits.OPERATION_QUERY_RECIPES}. Each 'key' is an opaque reference to " +
            "a JEI-defined recipe subtype, not a fingerprint of every NBT field. It expires on JEI reload or " +
            "reference-cache eviction; an 'unknown_key' result means you must search again. The candidates " +
            "are display data only - they do not say what " +
            "the player can craft, and they carry no machine conditions. Results are paged: read further pages " +
            "while 'has_more' is true. This never changes JEI's own search filter.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(JeiToolArguments.FILTER) {
                    put("type", "string")
                    put("minLength", 1)
                    put("maxLength", JeiToolLimits.MAX_FILTER_LENGTH)
                    put(
                        "description",
                        "Text that must appear in the ingredient's display name or resource id, 1 to " +
                            "${JeiToolLimits.MAX_FILTER_LENGTH} characters. Case insensitive, surrounding " +
                            "spaces are ignored.",
                    )
                }
                putJsonObject(JeiToolArguments.PAGE) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", JeiToolLimits.MAX_PAGE)
                    put("description", "Zero based page number. Defaults to 0, the first page.")
                }
                putJsonObject(JeiToolArguments.PAGE_SIZE) {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE)
                    put(
                        "description",
                        "Candidates per page, 1 to ${JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE}. Defaults to " +
                            "${JeiToolLimits.DEFAULT_INGREDIENT_PAGE_SIZE}.",
                    )
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(JeiToolArguments.FILTER))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = JeiTools.parameters(JeiToolLimits.OPERATION_FIND_INGREDIENTS, arguments)
        val query = validated { JeiToolArguments.findIngredients(parameters) }
        return gateway.findIngredients(query)
    }
}
