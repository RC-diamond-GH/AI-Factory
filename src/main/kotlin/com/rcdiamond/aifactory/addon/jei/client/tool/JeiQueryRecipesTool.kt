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
 * Advertises and validates one `jei_query_recipes` call.
 *
 * The description warns that a direct id does not select a subtype and that displayed slots are not a
 * crafting authority, because the model must not treat JEI's view as a guarantee; only the arguments are
 * resolved here, and the request goes to [gateway] for the current JEI runtime to answer.
 */
internal class JeiQueryRecipesTool(
    private val gateway: JeiQueryService,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = JeiToolLimits.OPERATION_QUERY_RECIPES,
        description = "Looks up the recipes, uses or catalyst entries JEI displays for one ingredient. Name " +
            "exactly one of '${JeiToolArguments.INGREDIENT_KEY}' (a runtime-scoped key from " +
            "${JeiToolLimits.OPERATION_FIND_INGREDIENTS}, which selects a JEI-defined recipe subtype) or " +
            "'${JeiToolArguments.INGREDIENT_ID}' (a resource id such as 'minecraft:stone'; the namespace " +
            "defaults to minecraft). A plain id selects a default stack, not every NBT or damage variant. " +
            "Use a search key to query a particular registered subtype. '${JeiToolArguments.INGREDIENT_TYPE}' only " +
            "applies when naming an id, and '${JeiToolArguments.DIRECTION}' chooses whether the ingredient is a " +
            "recipe output ('recipes'), an input ('uses') or a catalyst ('catalyst'). The returned slots are " +
            "display data, not a crafting authority: they do not prove the player can craft anything, and the " +
            "alternatives listed for one slot may be correlated (for example, rotations of the same input), so " +
            "they are not independent valid combinations. This tool does not extract machine tier, energy, " +
            "duration or probability; absent execution conditions must not be treated as zero or satisfied. " +
            "Output is bounded and truncated, and results are paged: read further pages while 'has_more' is true.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(JeiToolArguments.INGREDIENT_KEY) {
                    put("type", "string")
                    put("maxLength", JeiToolLimits.MAX_KEY_LENGTH)
                    put(
                        "description",
                        "A 'key' returned by ${JeiToolLimits.OPERATION_FIND_INGREDIENTS} in the current runtime. " +
                            "Prefer this when the JEI-defined subtype matters; it is not a full NBT fingerprint.",
                    )
                }
                putJsonObject(JeiToolArguments.INGREDIENT_ID) {
                    put("type", "string")
                    put("maxLength", JeiToolLimits.MAX_ID_LENGTH)
                    put("pattern", "^(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+\$")
                    put(
                        "description",
                        "Lowercase resource id, optionally namespaced; a missing namespace becomes 'minecraft'. " +
                            "Names the registry entry only, so it does not select one NBT or damage variant.",
                    )
                }
                putJsonObject(JeiToolArguments.INGREDIENT_TYPE) {
                    put("type", "string")
                    putJsonArray("enum") { JeiToolLimits.INGREDIENT_TYPES.sorted().forEach { add(JsonPrimitive(it)) } }
                    put(
                        "description",
                        "Whether an '${JeiToolArguments.INGREDIENT_ID}' names an item or a fluid. Defaults to " +
                            "'item'. Not accepted together with '${JeiToolArguments.INGREDIENT_KEY}'.",
                    )
                }
                putJsonObject(JeiToolArguments.DIRECTION) {
                    put("type", "string")
                    putJsonArray("enum") { JeiToolLimits.DIRECTIONS.sorted().forEach { add(JsonPrimitive(it)) } }
                    put(
                        "description",
                        "'recipes' (the ingredient is an output), 'uses' (it is an input) or 'catalyst'. " +
                            "Defaults to 'recipes'.",
                    )
                }
                putJsonObject(JeiToolArguments.CATEGORY_ID) {
                    put("type", "string")
                    put("maxLength", JeiToolLimits.MAX_ID_LENGTH)
                    put("pattern", "^(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+\$")
                    put(
                        "description",
                        "Optional lowercase recipe category resource id, for example 'minecraft:crafting'. Omit " +
                            "it to search every category.",
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
                    put("maximum", JeiToolLimits.MAX_RECIPE_PAGE_SIZE)
                    put(
                        "description",
                        "Recipes per page, 1 to ${JeiToolLimits.MAX_RECIPE_PAGE_SIZE}. Defaults to " +
                            "${JeiToolLimits.DEFAULT_RECIPE_PAGE_SIZE}.",
                    )
                }
            }
            putJsonArray("oneOf") {
                add(buildJsonObject {
                    putJsonArray("required") { add(JsonPrimitive(JeiToolArguments.INGREDIENT_KEY)) }
                    putJsonObject("not") {
                        putJsonArray("required") { add(JsonPrimitive(JeiToolArguments.INGREDIENT_TYPE)) }
                    }
                })
                add(buildJsonObject { putJsonArray("required") { add(JsonPrimitive(JeiToolArguments.INGREDIENT_ID)) } })
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = JeiTools.parameters(JeiToolLimits.OPERATION_QUERY_RECIPES, arguments)
        val query = validated { JeiToolArguments.queryRecipes(parameters) }
        return gateway.queryRecipes(query)
    }
}
