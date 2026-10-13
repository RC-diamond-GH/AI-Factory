package com.rcdiamond.aifactory.addon.ae2

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Validation and normalization for the three ME terminal operations.
 *
 * Arguments reaching the server are untrusted, so the server parses them again with the very same
 * code the client uses. Rejections are [IllegalArgumentException]s whose messages name the parameter
 * and the violated bound but never echo arbitrary model input; the client turns them into
 * `ToolArgumentException` observations and the server into `{ok:false,error:...}` replies.
 *
 * Every [toJson] emits the fully resolved values (defaults included), which is what the client sends.
 */
object Ae2ToolArguments {
    const val FILTER = "filter"
    const val PAGE = "page"
    const val PAGE_SIZE = "page_size"
    const val KEY = "key"
    const val PATH = "path"
    const val DEPTH = "depth"
    const val AMOUNT = "amount"

    /** `ae2_read_inventory` arguments. */
    class ReadInventory(val filter: String, val page: Int, val pageSize: Int) {
        fun toJson(): JsonObject = buildJsonObject {
            put(FILTER, filter)
            put(PAGE, page)
            put(PAGE_SIZE, pageSize)
        }
    }

    /** `ae2_item_details` arguments. */
    class ItemDetails(val key: String, val path: String, val depth: Int, val page: Int, val pageSize: Int) {
        fun toJson(): JsonObject = buildJsonObject {
            put(KEY, key)
            put(PATH, path)
            put(DEPTH, depth)
            put(PAGE, page)
            put(PAGE_SIZE, pageSize)
        }
    }

    /** `ae2_crafting_plan` arguments. */
    class CraftingPlan(val key: String, val amount: Long) {
        fun toJson(): JsonObject = buildJsonObject {
            put(KEY, key)
            put(AMOUNT, amount)
        }
    }

    fun readInventory(arguments: JsonObject): ReadInventory {
        rejectUnknown(arguments, setOf(FILTER, PAGE, PAGE_SIZE))
        return ReadInventory(
            filter = optionalFilter(arguments),
            page = optionalInt(arguments, PAGE, 0, 0, Ae2ToolLimits.MAX_PAGE),
            pageSize = optionalInt(arguments, PAGE_SIZE, Ae2ToolLimits.DEFAULT_PAGE_SIZE, 1, Ae2ToolLimits.MAX_PAGE_SIZE),
        )
    }

    fun itemDetails(arguments: JsonObject): ItemDetails {
        rejectUnknown(arguments, setOf(KEY, PATH, DEPTH, PAGE, PAGE_SIZE))
        return ItemDetails(
            key = requiredFingerprint(arguments),
            path = optionalPath(arguments),
            depth = optionalInt(arguments, DEPTH, Ae2ToolLimits.DEFAULT_NBT_DEPTH, 0, Ae2ToolLimits.MAX_NBT_DEPTH),
            page = optionalInt(arguments, PAGE, 0, 0, Ae2ToolLimits.MAX_PAGE),
            pageSize = optionalInt(arguments, PAGE_SIZE, Ae2ToolLimits.DEFAULT_PAGE_SIZE, 1, Ae2ToolLimits.MAX_PAGE_SIZE),
        )
    }

    fun craftingPlan(arguments: JsonObject): CraftingPlan {
        rejectUnknown(arguments, setOf(KEY, AMOUNT))
        return CraftingPlan(
            key = requiredFingerprint(arguments),
            amount = optionalLong(arguments, AMOUNT, Ae2ToolLimits.MIN_AMOUNT, Ae2ToolLimits.MIN_AMOUNT, Ae2ToolLimits.MAX_AMOUNT),
        )
    }

    private fun rejectUnknown(arguments: JsonObject, allowed: Set<String>) {
        val unknown = arguments.keys - allowed
        require(unknown.isEmpty()) {
            "unsupported parameter(s) ${unknown.sorted().joinToString(", ")}"
        }
    }

    private fun requiredFingerprint(arguments: JsonObject): String {
        val value = arguments[KEY]
        require(value is JsonPrimitive && value.isString) { "parameter '$KEY' must be a string item key" }
        val text = value.content
        require(Ae2ToolLimits.isItemKeyFingerprint(text)) {
            "parameter '$KEY' must be the ${Ae2ToolLimits.KEY_FINGERPRINT_LENGTH} character lowercase hex item key " +
                "returned by ${Ae2ToolLimits.OPERATION_READ_INVENTORY}"
        }
        return text
    }

    private fun optionalFilter(arguments: JsonObject): String {
        val value = arguments[FILTER] ?: return ""
        require(value is JsonPrimitive && value.isString) { "parameter '$FILTER' must be a string" }
        val text = value.content.trim()
        require(text.length <= Ae2ToolLimits.MAX_FILTER_LENGTH) {
            "parameter '$FILTER' must be at most ${Ae2ToolLimits.MAX_FILTER_LENGTH} characters"
        }
        return text
    }

    private fun optionalPath(arguments: JsonObject): String {
        val value = arguments[PATH] ?: return ""
        require(value is JsonPrimitive && value.isString) { "parameter '$PATH' must be a string" }
        val text = value.content.trim()
        require(text.length <= Ae2ToolLimits.MAX_PATH_LENGTH) {
            "parameter '$PATH' must be at most ${Ae2ToolLimits.MAX_PATH_LENGTH} characters"
        }
        // Structural validation lives with the navigator; it throws the same safe exception type.
        Ae2NbtView.parsePath(text)
        return text
    }

    private fun optionalInt(arguments: JsonObject, name: String, fallback: Int, minimum: Int, maximum: Int): Int {
        val value = arguments[name] ?: return fallback
        require(value is JsonPrimitive && !value.isString) { "parameter '$name' must be an integer" }
        val parsed = value.content.toIntOrNull() ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }

    private fun optionalLong(arguments: JsonObject, name: String, fallback: Long, minimum: Long, maximum: Long): Long {
        val value = arguments[name] ?: return fallback
        require(value is JsonPrimitive && !value.isString) { "parameter '$name' must be an integer" }
        val parsed = value.content.toLongOrNull() ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }
}
