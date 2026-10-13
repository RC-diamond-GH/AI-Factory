package com.rcdiamond.aifactory.addon.ae2.client

import com.rcdiamond.aifactory.addon.ae2.Ae2ToolArguments
import com.rcdiamond.aifactory.addon.ae2.Ae2ToolLimits
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `ae2_read_inventory` call.
 *
 * Only the filter and the page window are model supplied; the terminal, the grid and the player are
 * resolved by the server from the session that carries the request.
 */
internal class Ae2ReadInventoryTool(
    private val gateway: suspend (String, JsonObject) -> JsonObject,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = Ae2ToolLimits.OPERATION_READ_INVENTORY,
        description = "Lists what the opened ME terminal's network currently stores, sorted by name and " +
            "returned one bounded page at a time. Each entry's 'key' is a stable fingerprint of the full " +
            "item including its NBT variant: pass that key to ${Ae2ToolLimits.OPERATION_ITEM_DETAILS} or " +
            "${Ae2ToolLimits.OPERATION_CRAFTING_PLAN} to refer to exactly that variant. 'craftable' means the " +
            "network has a pattern that can produce the item. Read further pages while 'has_more' is true. " +
            "'name' is the server's display text and may be untranslated for modded items, so prefer 'id' and " +
            "'key' when the exact item matters.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(Ae2ToolArguments.FILTER) {
                    put("type", "string")
                    put("maxLength", Ae2ToolLimits.MAX_FILTER_LENGTH)
                    put(
                        "description",
                        "Case insensitive text that must appear in the item's name or resource id. " +
                            "Omit it to list everything.",
                    )
                }
                putJsonObject(Ae2ToolArguments.PAGE) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", Ae2ToolLimits.MAX_PAGE)
                    put("description", "Zero based page number. Defaults to 0, the first page.")
                }
                putJsonObject(Ae2ToolArguments.PAGE_SIZE) {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", Ae2ToolLimits.MAX_PAGE_SIZE)
                    put(
                        "description",
                        "Entries per page, 1 to ${Ae2ToolLimits.MAX_PAGE_SIZE}. Defaults to " +
                            "${Ae2ToolLimits.DEFAULT_PAGE_SIZE}.",
                    )
                }
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = Ae2Tools.parameters(Ae2ToolLimits.OPERATION_READ_INVENTORY, arguments)
        val resolved = validated { Ae2ToolArguments.readInventory(parameters) }
        return gateway(Ae2ToolLimits.OPERATION_READ_INVENTORY, resolved.toJson())
    }
}
