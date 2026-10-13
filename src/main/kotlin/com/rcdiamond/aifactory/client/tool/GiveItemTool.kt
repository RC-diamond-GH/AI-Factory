package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.GiveItemGateway
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `give_item` call.
 *
 * The tool owns only the schema and the argument policy: an id that looks like a resource location,
 * a count of 1..2304. The actual grant — permissions, registry lookup, target player — stays
 * authoritative on the server and is reached through [gateway].
 */
class GiveItemTool(private val gateway: GiveItemGateway) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = NAME,
        description = "Gives items to yourself, the player asking for them. " +
            "Use a lowercase resource location such as 'minecraft:diamond'. " +
            "Ask for one item id per call, and only what the player asked for.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(ITEM_ID) {
                    put("type", "string")
                    put("description", "Item resource location, for example 'minecraft:diamond_sword'.")
                }
                putJsonObject(COUNT) {
                    put("type", "integer")
                    put("description", "Number of items to give, $MIN_COUNT to $MAX_COUNT.")
                    put("minimum", MIN_COUNT)
                    put("maximum", MAX_COUNT)
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(ITEM_ID))
                add(JsonPrimitive(COUNT))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = arguments as? JsonObject ?: throw ToolArgumentException("$NAME arguments must be a JSON object")
        val unknown = parameters.keys - PARAMETERS
        if (unknown.isNotEmpty()) {
            throw ToolArgumentException("$NAME has no parameters named ${unknown.sorted().joinToString(", ")}")
        }
        val missing = PARAMETERS - parameters.keys
        if (missing.isNotEmpty()) {
            throw ToolArgumentException("$NAME requires ${missing.sorted().joinToString(", ")}")
        }
        val itemId = parameters.stringParameter(ITEM_ID)
        val count = parameters.intParameter(COUNT)
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw ToolArgumentException("$NAME count must be between $MIN_COUNT and $MAX_COUNT, got $count")
        }
        if (itemId.length > MAX_ITEM_ID_LENGTH) {
            throw ToolArgumentException("$NAME item_id must be at most $MAX_ITEM_ID_LENGTH characters")
        }
        if (!RESOURCE_LOCATION.matches(itemId)) {
            throw ToolArgumentException("$NAME item_id '$itemId' is not a lowercase resource location such as 'minecraft:diamond'")
        }
        val result = gateway.giveItem(itemId, count)
        return buildJsonObject {
            put(ITEM_ID, itemId)
            put(COUNT, count)
            put("success", result.success)
            put("message", result.message)
        }
    }

    /** Reads a required string parameter, rejecting numbers, booleans and null. */
    private fun JsonObject.stringParameter(name: String): String {
        val value = this[name]
        if (value !is JsonPrimitive || !value.isString) {
            throw ToolArgumentException("$NAME parameter '$name' must be a string")
        }
        return value.content
    }

    /** Reads a required integer parameter, rejecting strings, fractions and null. */
    private fun JsonObject.intParameter(name: String): Int {
        val value = this[name]
        if (value !is JsonPrimitive || value.isString) {
            throw ToolArgumentException("$NAME parameter '$name' must be an integer")
        }
        return value.content.toIntOrNull() ?: throw ToolArgumentException("$NAME parameter '$name' must be an integer")
    }

    companion object {
        const val NAME = "give_item"
        const val MIN_COUNT = 1
        const val MAX_COUNT = 2304

        private const val ITEM_ID = "item_id"
        private const val COUNT = "count"
        private const val MAX_ITEM_ID_LENGTH = 128

        private val PARAMETERS = setOf(ITEM_ID, COUNT)

        /** Namespace and path of a resource location; a bare path relies on the server's default namespace. */
        private val RESOURCE_LOCATION = Regex("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+")
    }
}
