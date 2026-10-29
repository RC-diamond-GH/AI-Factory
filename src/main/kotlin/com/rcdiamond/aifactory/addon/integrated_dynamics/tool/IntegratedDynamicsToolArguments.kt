package com.rcdiamond.aifactory.addon.integrated_dynamics.tool

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Validation and normalization for the three read-only Integrated Dynamics operations.
 *
 * Arguments reaching a tool come straight from the model, so each one is checked here and turned into
 * the values the gateway expects; the fully resolved values (defaults included) are what the client
 * sends and what the server parses again with this very same code. Rejections are
 * [IllegalArgumentException]s that name the parameter and the violated bound but never echo arbitrary
 * model input: the client turns them into a model-correctable observation instead of a thrown failure.
 *
 * The listing operations share one strict key set ([FILTER], [PAGE], [PAGE_SIZE]) so a filter the
 * client accepts is always a filter the server accepts. [networkInfo] takes no parameters at all.
 */
object IntegratedDynamicsToolArguments {
    const val FILTER = "filter"
    const val PAGE = "page"
    const val PAGE_SIZE = "page_size"

    private val LIST_PARAMETERS = setOf(FILTER, PAGE, PAGE_SIZE)

    /** The resolved listing window `id_list_parts` and `id_list_variables` both use. */
    class Listing(val filter: String, val page: Int, val pageSize: Int) {
        fun toJson(): JsonObject = buildJsonObject {
            put(FILTER, filter)
            put(PAGE, page)
            put(PAGE_SIZE, pageSize)
        }
    }

    /** `id_network_info` arguments: an object that must carry no parameters. */
    fun networkInfo(arguments: JsonObject): JsonObject {
        rejectUnknown(arguments, emptySet())
        return buildJsonObject {}
    }

    /** `id_list_parts` arguments. */
    fun listParts(arguments: JsonObject): Listing = listing(arguments)

    /** `id_list_variables` arguments. */
    fun listVariables(arguments: JsonObject): Listing = listing(arguments)

    private fun listing(arguments: JsonObject): Listing {
        rejectUnknown(arguments, LIST_PARAMETERS)
        return Listing(
            filter = optionalFilter(arguments),
            page = optionalInt(arguments, PAGE, 0, 0, IntegratedDynamicsToolLimits.MAX_PAGE),
            pageSize = optionalInt(
                arguments,
                PAGE_SIZE,
                IntegratedDynamicsToolLimits.DEFAULT_PAGE_SIZE,
                1,
                IntegratedDynamicsToolLimits.MAX_PAGE_SIZE,
            ),
        )
    }

    private fun rejectUnknown(arguments: JsonObject, allowed: Set<String>) {
        val unknown = arguments.keys - allowed
        require(unknown.isEmpty()) {
            "unsupported parameter(s) ${unknown.sorted().joinToString(", ")}"
        }
    }

    private fun optionalFilter(arguments: JsonObject): String {
        val value = arguments[FILTER] ?: return ""
        require(value is JsonPrimitive && value.isString) { "parameter '$FILTER' must be a string" }
        val text = value.content.trim()
        require(text.length <= IntegratedDynamicsToolLimits.MAX_FILTER_LENGTH) {
            "parameter '$FILTER' must be at most ${IntegratedDynamicsToolLimits.MAX_FILTER_LENGTH} characters"
        }
        return text
    }

    private fun optionalInt(arguments: JsonObject, name: String, fallback: Int, minimum: Int, maximum: Int): Int {
        val value = arguments[name] ?: return fallback
        require(value is JsonPrimitive && !value.isString) { "parameter '$name' must be an integer" }
        val parsed = value.content.toIntOrNull() ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }
}
