package com.rcdiamond.aifactory.addon.jei.client.tool

import com.rcdiamond.aifactory.addon.jei.client.JeiIngredientQuery
import com.rcdiamond.aifactory.addon.jei.client.JeiRecipeQuery
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Validation and normalization for the two JEI query tools.
 *
 * Arguments reaching a tool come straight from the model, so each one is checked here and turned into
 * the data class the gateway expects; the fully resolved values (defaults included) are what the
 * gateway receives. Rejections are [IllegalArgumentException]s that name the parameter and the
 * violated rule but never echo arbitrary model input: the tool turns them into a model-correctable
 * observation instead of a thrown failure.
 *
 * Resource ids are canonicalized the same way for every caller: case is significant and must already
 * be lowercase, and a missing namespace becomes [JeiToolLimits.DEFAULT_NAMESPACE], so `stone` and
 * `minecraft:stone` name the same entry.
 */
object JeiToolArguments {
    const val FILTER = "filter"
    const val PAGE = "page"
    const val PAGE_SIZE = "page_size"
    const val INGREDIENT_KEY = "ingredient_key"
    const val INGREDIENT_TYPE = "ingredient_type"
    const val INGREDIENT_ID = "ingredient_id"
    const val DIRECTION = "direction"
    const val CATEGORY_ID = "category_id"

    private val SEARCH_PARAMETERS = setOf(FILTER, PAGE, PAGE_SIZE)
    private val RECIPE_PARAMETERS = setOf(INGREDIENT_KEY, INGREDIENT_TYPE, INGREDIENT_ID, DIRECTION, CATEGORY_ID, PAGE, PAGE_SIZE)
    private val RESOURCE_ID = Regex("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+")

    /** `jei_find_ingredients` arguments. */
    fun findIngredients(arguments: JsonObject): JeiIngredientQuery {
        rejectUnknown(arguments, SEARCH_PARAMETERS)
        return JeiIngredientQuery(
            filter = requiredFilter(arguments),
            page = optionalInt(arguments, PAGE, 0, 0, JeiToolLimits.MAX_PAGE),
            pageSize = optionalInt(
                arguments, PAGE_SIZE, JeiToolLimits.DEFAULT_INGREDIENT_PAGE_SIZE,
                1, JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE,
            ),
        )
    }

    /** `jei_query_recipes` arguments. */
    fun queryRecipes(arguments: JsonObject): JeiRecipeQuery {
        rejectUnknown(arguments, RECIPE_PARAMETERS)
        val hasKey = arguments[INGREDIENT_KEY] != null
        val hasId = arguments[INGREDIENT_ID] != null
        require(hasKey != hasId) {
            "provide exactly one of '$INGREDIENT_KEY' or '$INGREDIENT_ID', not " +
                (if (hasKey) "both" else "neither")
        }
        require(!(hasKey && arguments[INGREDIENT_TYPE] != null)) {
            "parameter '$INGREDIENT_TYPE' is only valid together with '$INGREDIENT_ID'"
        }
        return JeiRecipeQuery(
            ingredientKey = if (hasKey) requiredKey(arguments, INGREDIENT_KEY) else null,
            ingredientType = optionalEnum(arguments, INGREDIENT_TYPE, "item", JeiToolLimits.INGREDIENT_TYPES),
            ingredientId = optionalResourceId(arguments, INGREDIENT_ID),
            direction = optionalEnum(arguments, DIRECTION, "recipes", JeiToolLimits.DIRECTIONS),
            categoryId = optionalResourceId(arguments, CATEGORY_ID),
            page = optionalInt(arguments, PAGE, 0, 0, JeiToolLimits.MAX_PAGE),
            pageSize = optionalInt(
                arguments, PAGE_SIZE, JeiToolLimits.DEFAULT_RECIPE_PAGE_SIZE,
                1, JeiToolLimits.MAX_RECIPE_PAGE_SIZE,
            ),
        )
    }

    private fun rejectUnknown(arguments: JsonObject, allowed: Set<String>) {
        val unknown = arguments.keys - allowed
        require(unknown.isEmpty()) {
            "unsupported parameter(s) ${unknown.sorted().joinToString(", ")}"
        }
    }

    private fun requiredFilter(arguments: JsonObject): String {
        val value = arguments[FILTER]
            ?: throw IllegalArgumentException("parameter '$FILTER' is required")
        require(value is JsonPrimitive && value.isString) { "parameter '$FILTER' must be a string" }
        val text = value.content
        require(text.length <= JeiToolLimits.MAX_FILTER_LENGTH) {
            "parameter '$FILTER' must be at most ${JeiToolLimits.MAX_FILTER_LENGTH} characters"
        }
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "parameter '$FILTER' must not be blank" }
        return trimmed
    }

    private fun requiredKey(arguments: JsonObject, name: String): String {
        val value = arguments[name]
        require(value is JsonPrimitive && value.isString) { "parameter '$name' must be a string key" }
        val text = value.content
        require(text.isNotBlank()) { "parameter '$name' must not be blank" }
        require(text.length <= JeiToolLimits.MAX_KEY_LENGTH) {
            "parameter '$name' must be at most ${JeiToolLimits.MAX_KEY_LENGTH} characters"
        }
        return text
    }

    private fun optionalResourceId(arguments: JsonObject, name: String): String? {
        val value = arguments[name] ?: return null
        require(value is JsonPrimitive && value.isString) { "parameter '$name' must be a string resource id" }
        val text = value.content
        require(text.length <= JeiToolLimits.MAX_ID_LENGTH) {
            "parameter '$name' must be at most ${JeiToolLimits.MAX_ID_LENGTH} characters"
        }
        return canonicalResourceId(name, text)
    }

    /** Lowercase, optionally namespaced resource id; the namespace defaults to `minecraft`. */
    private fun canonicalResourceId(name: String, value: String): String {
        require(RESOURCE_ID.matches(value)) { "parameter '$name' must be a lowercase resource id" }
        val canonical = if (':' in value) value else "${JeiToolLimits.DEFAULT_NAMESPACE}:$value"
        require(canonical.length <= JeiToolLimits.MAX_ID_LENGTH) {
            "parameter '$name' must be at most ${JeiToolLimits.MAX_ID_LENGTH} characters including its namespace"
        }
        return canonical
    }

    private fun optionalEnum(
        arguments: JsonObject,
        name: String,
        fallback: String,
        allowed: Set<String>,
    ): String {
        val value = arguments[name] ?: return fallback
        require(value is JsonPrimitive && value.isString) { "parameter '$name' must be a string" }
        val text = value.content
        require(text in allowed) {
            "parameter '$name' must be one of ${allowed.sorted().joinToString(", ")}"
        }
        return text
    }

    private fun optionalInt(arguments: JsonObject, name: String, fallback: Int, minimum: Int, maximum: Int): Int {
        val value = arguments[name] ?: return fallback
        require(value is JsonPrimitive && !value.isString) { "parameter '$name' must be an integer" }
        val parsed = value.content.toIntOrNull()
            ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }
}
