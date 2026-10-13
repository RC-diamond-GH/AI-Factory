package com.rcdiamond.aifactory.addon.ae2.client

import com.rcdiamond.aifactory.addon.ae2.Ae2ToolArguments
import com.rcdiamond.aifactory.addon.ae2.Ae2ToolLimits
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
 * Advertises and validates one `ae2_item_details` call.
 *
 * The key must be a fingerprint the inventory listed, so variants of one item stay distinguishable;
 * the server re-resolves it against the live network instead of trusting the string.
 */
internal class Ae2ItemDetailsTool(
    private val gateway: suspend (String, JsonObject) -> JsonObject,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = Ae2ToolLimits.OPERATION_ITEM_DETAILS,
        description = "Describes one key returned by ${Ae2ToolLimits.OPERATION_READ_INVENTORY}: its resource " +
            "id, name, amount stored in the network, whether it is craftable, and a bounded, navigable view of " +
            "its NBT variant. Use 'path' to look inside the NBT, for example 'tag.display.Name' or " +
            "'tag.Enchantments[0]'; a path that does not exist answers with found=false and shows the deepest " +
            "container that does exist. 'depth' limits how many levels are rendered and 'page' with 'page_size' " +
            "page the children of the selected node. Oversized data is shortened and marked with '_type', " +
            "'_shown', '_total', '_length' and '_values' fields rather than being sent whole, and 'max_stack_size' " +
            "and 'damaged' are reported for items.",
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
                putJsonObject(Ae2ToolArguments.PATH) {
                    put("type", "string")
                    put("maxLength", Ae2ToolLimits.MAX_PATH_LENGTH)
                    put(
                        "description",
                        "NBT path relative to the key, such as 'tag.display.Name', 'tag.Enchantments[0]' or " +
                            "'tag.Items[3].id'. Escape a literal dot in a name as '${'\\'}.'. " +
                            "Omit it to view the whole key with depth and pagination applied.",
                    )
                }
                putJsonObject(Ae2ToolArguments.DEPTH) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", Ae2ToolLimits.MAX_NBT_DEPTH)
                    put(
                        "description",
                        "Levels of nesting to render below the selected node, 0 to " +
                            "${Ae2ToolLimits.MAX_NBT_DEPTH}. Defaults to ${Ae2ToolLimits.DEFAULT_NBT_DEPTH}.",
                    )
                }
                putJsonObject(Ae2ToolArguments.PAGE) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", Ae2ToolLimits.MAX_PAGE)
                    put("description", "Zero based page of the selected node's children. Defaults to 0.")
                }
                putJsonObject(Ae2ToolArguments.PAGE_SIZE) {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", Ae2ToolLimits.MAX_PAGE_SIZE)
                    put(
                        "description",
                        "Children per page, 1 to ${Ae2ToolLimits.MAX_PAGE_SIZE}. Defaults to " +
                            "${Ae2ToolLimits.DEFAULT_PAGE_SIZE}.",
                    )
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(Ae2ToolArguments.KEY))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = Ae2Tools.parameters(Ae2ToolLimits.OPERATION_ITEM_DETAILS, arguments)
        val resolved = validated { Ae2ToolArguments.itemDetails(parameters) }
        return gateway(Ae2ToolLimits.OPERATION_ITEM_DETAILS, resolved.toJson())
    }
}
