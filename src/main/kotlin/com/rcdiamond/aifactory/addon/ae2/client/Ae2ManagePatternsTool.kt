package com.rcdiamond.aifactory.addon.ae2.client

import com.rcdiamond.aifactory.addon.ae2.Ae2PatternToolArguments
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
 * Advertises and validates one `ae2_manage_patterns` call.
 *
 * The tool is the ME Pattern Access Terminal as a tool - plus installing a pattern into a container of the
 * current network. Both actions are described in full, including the side effects of `encode_install`, because
 * the model has to know that an install writes into a real machine and, for an empty slot, consumes a real
 * blank pattern from the network.
 *
 * Nothing here touches Applied Energistics or Minecraft: the arguments are validated by the same shared code
 * the server executor uses, and the request is handed to [gateway].
 */
internal class Ae2ManagePatternsTool(
    private val gateway: suspend (String, JsonObject) -> JsonObject,
) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = Ae2ToolLimits.OPERATION_MANAGE_PATTERNS,
        description = buildString {
            append(
                "Reads and edits the patterns of the opened ME terminal's network, like AE2's ME Pattern " +
                    "Access Terminal plus the ability to install a pattern. Two actions:\n" +
                    "'${Ae2PatternToolArguments.ACTION_VIEW}' is read only. It lists the network's active " +
                    "pattern containers - each with a 'dimension' and 'container' id (position and side), " +
                    "name, slot count and 'empty_slots' - and one entry per encoded pattern with its " +
                    "'pattern_key', decoded 'mode' (crafting, processing, stonecutting, smithing), 'editable', " +
                    "recipe and its real inputs/outputs. A slot holding a blank pattern reports mode 'blank', " +
                    "an encoded pattern whose data no longer decodes reports 'invalid' - both are still " +
                    "replaceable, since they hold pattern items - and an item that is not a pattern at all " +
                    "reports editable false and is never touched. " +
                    "It also reports 'blank_patterns', how many blank " +
                    "patterns the network can really give out. Entries are filtered by 'filter' (a case " +
                    "insensitive substring of container name, item id or name, mode, or an input/output id) " +
                    "and paged; read further pages while 'has_more' is true.\n" +
                    "'${Ae2PatternToolArguments.ACTION_ENCODE_INSTALL}' encodes one pattern and writes it into " +
                    "one slot of one container of this same network. You MUST view first: it requires the " +
                    "'dimension' and 'container' id, the 'slot', and 'expected' - the 'pattern_key' reported " +
                    "for that slot, or '${Ae2PatternToolArguments.EXPECTED_EMPTY}' for one of the container's " +
                    "empty_slots. If the slot no longer holds exactly that, the install is refused instead of " +
                    "overwriting anything. An install changes the world: an empty slot consumes one blank " +
                    "pattern taken from this network (so it is refused when the network has none - instead " +
                    "re-encode an existing pattern item), a slot that already holds a pattern is re-encoded in " +
                    "place, and AE2 then saves that container and refreshes its crafting patterns. A slot " +
                    "holding anything that is not a blank or encoded pattern is never touched, and only " +
                    "containers that are active, visible and interactive are edited.\n" +
                    "'mode' selects what is encoded; parameters of the other modes are rejected:\n" +
                    "- '${Ae2PatternToolArguments.MODE_CRAFTING}': 'grid' with up to " +
                    "${Ae2PatternToolArguments.MAX_CRAFTING_SLOTS} entries (left to right, top to bottom; null " +
                    "for an empty slot) naming the item in each slot. The recipe is looked up in this world " +
                    "and the pattern is encoded with that recipe's own output, so a recipe that does not exist " +
                    "cannot be encoded; 'substitute' allows equivalent items and 'fluid_substitute' allows a " +
                    "fluid for the container item (a bucket of water, for example; on by default). 'recipe' " +
                    "can pin an exact recipe id.\n" +
                    "- '${Ae2PatternToolArguments.MODE_PROCESSING}': 'inputs' and 'outputs' arrays of " +
                    "{'slot', 'key', 'amount'} describing a custom process (items or fluids). 'slot' defaults " +
                    "to the entry's position; the primary output must be in slot 0. Any key can be used, " +
                    "because a processing pattern has no recipe to check.\n" +
                    "- '${Ae2PatternToolArguments.MODE_STONECUTTING}': 'input' plus the 'output' to cut it " +
                    "into (required when several outputs match); both must be real items of a real stonecutter " +
                    "recipe of this world.\n" +
                    "- '${Ae2PatternToolArguments.MODE_SMITHING}': 'template', 'base' and 'addition'; the real " +
                    "smithing recipe's own output is what gets encoded.\n",
            )
            append(
                "A key is either the 64 character 'key' a view reported - always use that for anything with " +
                    "NBT, and it is re-resolved against the live network and its patterns - or " +
                    "{'item': 'minecraft:stone'} / {'fluid': 'minecraft:water'} for a plain entry without NBT. " +
                    "Identifiers use 'namespace:path'. Results are snapshots: view again after any change.",
            )
        },
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(Ae2PatternToolArguments.ACTION) {
                    put("type", "string")
                    putJsonArray("enum") {
                        add(JsonPrimitive(Ae2PatternToolArguments.ACTION_VIEW))
                        add(JsonPrimitive(Ae2PatternToolArguments.ACTION_ENCODE_INSTALL))
                    }
                    put(
                        "description",
                        "'${Ae2PatternToolArguments.ACTION_VIEW}' reads the containers and patterns, " +
                            "'${Ae2PatternToolArguments.ACTION_ENCODE_INSTALL}' writes one encoded pattern " +
                            "into a container slot. Required.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.FILTER) {
                    put("type", "string")
                    put("maxLength", Ae2ToolLimits.MAX_FILTER_LENGTH)
                    put(
                        "description",
                        "Case insensitive text that must appear in a pattern's container name, item id or " +
                            "name, mode, or one of its input/output ids. Omit it to list everything.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.PAGE) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", Ae2ToolLimits.MAX_PAGE)
                    put("description", "Zero based page number. Defaults to 0.")
                }
                putJsonObject(Ae2PatternToolArguments.PAGE_SIZE) {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", Ae2ToolLimits.MAX_PAGE_SIZE)
                    put(
                        "description",
                        "Patterns per page, 1 to ${Ae2ToolLimits.MAX_PAGE_SIZE}. Defaults to " +
                            "${Ae2ToolLimits.DEFAULT_PAGE_SIZE}.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.SHOW) {
                    put("type", "string")
                    putJsonArray("enum") {
                        add(JsonPrimitive(Ae2PatternToolArguments.SHOW_VISIBLE))
                        add(JsonPrimitive(Ae2PatternToolArguments.SHOW_NOT_FULL))
                        add(JsonPrimitive(Ae2PatternToolArguments.SHOW_ALL))
                    }
                    put(
                        "description",
                        "'${Ae2PatternToolArguments.SHOW_VISIBLE}' (default) lists the containers that opt " +
                            "into the pattern access terminal, '${Ae2PatternToolArguments.SHOW_NOT_FULL}' " +
                            "only those that still have an empty slot, '${Ae2PatternToolArguments.SHOW_ALL}' " +
                            "every pattern container of the network. Only visible containers can be edited.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.CONTAINER) {
                    put("type", "string")
                    put("maxLength", Ae2PatternToolArguments.MAX_CONTAINER_ID_LENGTH)
                    put(
                        "description",
                        "A container id the view reported, such as '12,64,-3/block' or '12,64,-3/up'. " +
                            "Required to install, optional for a view, where it restricts the listing to that " +
                            "one container.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.DIMENSION) {
                    put("type", "string")
                    put("maxLength", Ae2PatternToolArguments.MAX_ID_LENGTH)
                    put("description", "Required for encode_install: container dimension exactly as reported by view.")
                }
                putJsonObject(Ae2PatternToolArguments.SLOT) {
                    put("type", "integer")
                    put("minimum", 0)
                    put("maximum", Ae2PatternToolArguments.MAX_TARGET_SLOT)
                    put(
                        "description",
                        "The container slot to write into. Required to install; it must be one of the " +
                            "container's 'empty_slots' or the slot of a pattern the view listed.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.EXPECTED) {
                    put("type", "string")
                    put("maxLength", Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
                    put(
                        "description",
                        "What the target slot must currently hold: the 'pattern_key' the view reported for " +
                            "it, or '${Ae2PatternToolArguments.EXPECTED_EMPTY}' for an empty slot. Required " +
                            "to install.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.MODE) {
                    put("type", "string")
                    putJsonArray("enum") {
                        add(JsonPrimitive(Ae2PatternToolArguments.MODE_CRAFTING))
                        add(JsonPrimitive(Ae2PatternToolArguments.MODE_PROCESSING))
                        add(JsonPrimitive(Ae2PatternToolArguments.MODE_STONECUTTING))
                        add(JsonPrimitive(Ae2PatternToolArguments.MODE_SMITHING))
                    }
                    put("description", "Which kind of pattern to encode. Required to install.")
                }
                putJsonObject(Ae2PatternToolArguments.GRID) {
                    put("type", "array")
                    put("maxItems", Ae2PatternToolArguments.MAX_CRAFTING_SLOTS)
                    put(
                        "items",
                        keySchema(
                            "One crafting grid slot: a key, or null for an empty slot.",
                            nullable = true,
                        ),
                    )
                    put(
                        "description",
                        "Mode '${Ae2PatternToolArguments.MODE_CRAFTING}' only: the crafting grid, up to " +
                            "${Ae2PatternToolArguments.MAX_CRAFTING_SLOTS} keys, row by row, null for an " +
                            "empty slot. Each entry must be an item.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.INPUTS) {
                    put("type", "array")
                    put("items", stackEntrySchema())
                    put(
                        "description",
                        "Mode '${Ae2PatternToolArguments.MODE_PROCESSING}' only: the inputs of the process, " +
                            "at most ${Ae2PatternToolArguments.MAX_PROCESSING_INPUT_SLOT + 1} entries.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.OUTPUTS) {
                    put("type", "array")
                    put("items", stackEntrySchema())
                    put(
                        "description",
                        "Mode '${Ae2PatternToolArguments.MODE_PROCESSING}' only: the outputs of the process, " +
                            "at most ${Ae2PatternToolArguments.MAX_PROCESSING_OUTPUT_SLOT + 1} entries, the " +
                            "primary one in slot 0.",
                    )
                }
                put(
                    Ae2PatternToolArguments.INPUT,
                    keySchema("Mode '${Ae2PatternToolArguments.MODE_STONECUTTING}' only: the item to cut."),
                )
                put(
                    Ae2PatternToolArguments.OUTPUT,
                    keySchema(
                        "Optional for '${Ae2PatternToolArguments.MODE_CRAFTING}', " +
                            "'${Ae2PatternToolArguments.MODE_STONECUTTING}' and " +
                            "'${Ae2PatternToolArguments.MODE_SMITHING}': the output you expect, which must " +
                            "equal what the real recipe produces. It never allows a fabricated output; for " +
                            "stonecutting it selects the recipe when several match.",
                        nullable = true,
                    ),
                )
                put(
                    Ae2PatternToolArguments.TEMPLATE,
                    keySchema("Mode '${Ae2PatternToolArguments.MODE_SMITHING}' only: the template item."),
                )
                put(
                    Ae2PatternToolArguments.BASE,
                    keySchema("Mode '${Ae2PatternToolArguments.MODE_SMITHING}' only: the base item."),
                )
                put(
                    Ae2PatternToolArguments.ADDITION,
                    keySchema("Mode '${Ae2PatternToolArguments.MODE_SMITHING}' only: the addition item."),
                )
                putJsonObject(Ae2PatternToolArguments.RECIPE) {
                    put("type", "string")
                    put("maxLength", Ae2PatternToolArguments.MAX_ID_LENGTH)
                    put(
                        "description",
                        "Optional for '${Ae2PatternToolArguments.MODE_CRAFTING}', " +
                            "'${Ae2PatternToolArguments.MODE_STONECUTTING}' and " +
                            "'${Ae2PatternToolArguments.MODE_SMITHING}': pin one exact recipe by id instead of " +
                            "letting this world's recipes be searched.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.SUBSTITUTE) {
                    put("type", "boolean")
                    put(
                        "description",
                        "Optional, default false: allow equivalent items (ore dictionary style) where the " +
                            "encoded ones are not available. Crafting, stonecutting and smithing only.",
                    )
                }
                putJsonObject(Ae2PatternToolArguments.FLUID_SUBSTITUTE) {
                    put("type", "boolean")
                    put(
                        "description",
                        "Optional, default true: allow a fluid for the container item a crafting pattern " +
                            "encodes. Crafting only.",
                    )
                }
            }
            putJsonArray("required") { add(JsonPrimitive(Ae2PatternToolArguments.ACTION)) }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = Ae2Tools.parameters(Ae2ToolLimits.OPERATION_MANAGE_PATTERNS, arguments)
        val resolved = validated { Ae2PatternToolArguments.parse(parameters) }
        return gateway(Ae2ToolLimits.OPERATION_MANAGE_PATTERNS, resolved.toJson())
    }
}

/**
 * One key: the fingerprint string a view reports (the only way to name a variant that carries NBT), or a plain
 * item / fluid registry entry. [nullable] adds JSON null, for the slots of a crafting grid and for the outputs
 * that may be left out.
 */
private fun keySchema(description: String, nullable: Boolean = false): JsonObject = buildJsonObject {
    put("description", description)
    putJsonArray("oneOf") {
        add(fingerprintSchema())
        add(registryIdSchema(Ae2PatternToolArguments.ITEM))
        add(registryIdSchema(Ae2PatternToolArguments.FLUID))
        if (nullable) add(buildJsonObject { put("type", "null") })
    }
}

private fun fingerprintSchema(): JsonObject = buildJsonObject {
    put("type", "string")
    put("minLength", Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
    put("maxLength", Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
    put("pattern", "^[0-9a-f]{${Ae2ToolLimits.KEY_FINGERPRINT_LENGTH}}$")
}

private fun registryIdSchema(field: String): JsonObject = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonObject("properties") {
        putJsonObject(field) {
            put("type", "string")
            put("maxLength", Ae2PatternToolArguments.MAX_ID_LENGTH)
            put("pattern", "^[a-z0-9_.-]+:[a-z0-9/._-]+$")
        }
    }
    putJsonArray("required") { add(JsonPrimitive(field)) }
}

/** One sparse entry of a processing pattern. */
private fun stackEntrySchema(): JsonObject = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonObject("properties") {
        putJsonObject(Ae2PatternToolArguments.SLOT) {
            put("type", "integer")
            put("minimum", 0)
            put("maximum", Ae2PatternToolArguments.MAX_PROCESSING_INPUT_SLOT)
            put("description", "Position inside the pattern. Defaults to the entry's position in the array.")
        }
        put(Ae2PatternToolArguments.KEY, keySchema("The item or fluid for this entry."))
        putJsonObject(Ae2PatternToolArguments.AMOUNT) {
            put("type", "integer")
            put("minimum", 1)
            put("maximum", Ae2ToolLimits.MAX_AMOUNT)
            put("description", "How many, with fluid amounts in mB. Defaults to 1.")
        }
    }
    putJsonArray("required") { add(JsonPrimitive(Ae2PatternToolArguments.KEY)) }
}
