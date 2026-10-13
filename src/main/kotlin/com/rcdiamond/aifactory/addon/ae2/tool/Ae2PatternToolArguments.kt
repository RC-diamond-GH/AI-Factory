package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.abs

/**
 * Validation and normalization for [Ae2ToolLimits.OPERATION_MANAGE_PATTERNS].
 *
 * Kept apart from [Ae2ToolArguments] because this operation has its own parameter space (modes, key
 * references, sparse pattern slots) and its own bounds; nothing here references AE2 or Minecraft, so the
 * client tool and the server executor validate with the very same code, and the rules are unit testable
 * without booting the game.
 *
 * ## Two actions
 * - `view`: read the pattern containers of the terminal's grid (filter/page/show) - no side effect.
 * - `encode_install`: encode one pattern and write it into a named container slot. Every install names the
 *   container, the slot and the *expected* current content of that slot, so an install can never silently
 *   overwrite a slot that changed between the `view` and the write.
 *
 * ## How a key is named
 * A key is either
 * - a fingerprint string: the 64 character lowercase hex id of the full AE key (including NBT) exactly as a
 *   previous [Ae2ToolLimits.OPERATION_MANAGE_PATTERNS] `view` (or [Ae2ToolLimits.OPERATION_READ_INVENTORY])
 *   reported it, or
 * - `{"item": "minecraft:stone"}` / `{"fluid": "minecraft:water"}` for a plain, NBT free registry entry.
 *
 * Arbitrary SNBT is never accepted: an NBT carrying variant can only be reproduced by a fingerprint, which
 * the executor re-resolves against the live network and the live pattern contents of the same terminal.
 *
 * Rejections are [IllegalArgumentException]s that name the parameter and the violated bound but never echo
 * arbitrary model input; the client turns them into `ToolArgumentException` observations and the server into
 * `{ok:false,error:...}` replies. [Request.toJson] emits the fully resolved values (defaults included), which
 * is what the client sends and the server parses again.
 */
object Ae2PatternToolArguments {
    // --- action ---------------------------------------------------------------

    const val ACTION = "action"

    /** Reads the grid's pattern containers. */
    const val ACTION_VIEW = "view"

    /** Encodes one pattern and installs it into a named container slot. */
    const val ACTION_ENCODE_INSTALL = "encode_install"

    // --- view parameters ------------------------------------------------------

    const val FILTER = "filter"
    const val PAGE = "page"
    const val PAGE_SIZE = "page_size"
    const val SHOW = "show"

    /** Mirrors AE2's `ShowPatternProviders.VISIBLE`: containers that opt into the terminal. */
    const val SHOW_VISIBLE = "visible"

    /** Mirrors AE2's `ShowPatternProviders.NOT_FULL`: visible containers that still have an empty slot. */
    const val SHOW_NOT_FULL = "not_full"

    /** Mirrors AE2's `ShowPatternProviders.ALL`. */
    const val SHOW_ALL = "all"

    val SHOWS: Set<String> = setOf(SHOW_VISIBLE, SHOW_NOT_FULL, SHOW_ALL)

    // --- install parameters ---------------------------------------------------

    const val CONTAINER = "container"
    const val DIMENSION = "dimension"
    const val SLOT = "slot"
    const val EXPECTED = "expected"

    /** [EXPECTED] value meaning "this slot must be empty right now". */
    const val EXPECTED_EMPTY = "empty"

    const val MODE = "mode"
    const val MODE_CRAFTING = "crafting"
    const val MODE_PROCESSING = "processing"
    const val MODE_STONECUTTING = "stonecutting"
    const val MODE_SMITHING = "smithing"

    val MODES: Set<String> = setOf(MODE_CRAFTING, MODE_PROCESSING, MODE_STONECUTTING, MODE_SMITHING)

    /** Mode specific payload names. */
    const val GRID = "grid"
    const val INPUTS = "inputs"
    const val OUTPUTS = "outputs"
    const val KEY = "key"
    const val AMOUNT = "amount"
    const val ITEM = "item"
    const val FLUID = "fluid"
    const val INPUT = "input"
    const val OUTPUT = "output"
    const val TEMPLATE = "template"
    const val BASE = "base"
    const val ADDITION = "addition"
    const val RECIPE = "recipe"
    const val SUBSTITUTE = "substitute"
    const val FLUID_SUBSTITUTE = "fluid_substitute"

    // --- bounds ---------------------------------------------------------------

    /** Longest accepted container id (`x,y,z/side`). */
    const val MAX_CONTAINER_ID_LENGTH = 48

    /** Longest accepted registry id / recipe id. */
    const val MAX_ID_LENGTH = 128

    /** Absolute world border; a container id outside of it cannot exist. */
    const val MAX_COORDINATE = 30_000_000

    /** Hard cap on the reported target slot; the real bound is the container's slot count. */
    const val MAX_TARGET_SLOT = 255

    /** AE2's own [appeng.crafting.pattern.AECraftingPattern.CRAFTING_GRID_SLOTS]. */
    const val MAX_CRAFTING_SLOTS = 9

    /** AE2's own `AEProcessingPattern.MAX_INPUT_SLOTS - 1`. */
    const val MAX_PROCESSING_INPUT_SLOT = 80

    /** AE2's own `AEProcessingPattern.MAX_OUTPUT_SLOTS - 1`. */
    const val MAX_PROCESSING_OUTPUT_SLOT = 26

    /** Registry id / recipe id syntax; identical to vanilla's `ResourceLocation` grammar. */
    private val REGISTRY_ID = Regex("^[a-z0-9_.-]+:[a-z0-9/._-]+$")

    /** `<x>,<y>,<z>/<side>` where side is a vanilla direction name, or `block` for a block form provider. */
    private val CONTAINER_ID = Regex("^(-?\\d{1,9}),(-?\\d{1,9}),(-?\\d{1,9})/(block|up|down|north|south|west|east)$")

    // --- key references -------------------------------------------------------

    /** One AE key named by fingerprint (full variant, NBT included) or by plain registry entry. */
    sealed interface KeyRef {
        /** The 64 character fingerprint of a full AE key. */
        data class Fingerprint(val value: String) : KeyRef

        /** A plain registry entry without NBT; [fluid] selects the fluid registry over the item registry. */
        data class RegistryId(val fluid: Boolean, val id: String) : KeyRef
    }

    /** One sparse pattern entry: the slot inside the pattern, the key and how many of it. */
    class StackEntry(val slot: Int, val key: KeyRef, val amount: Long)

    /** A fully validated install request. */
    class Install(
        val container: ContainerAddress,
        val dimension: String,
        val slot: Int,
        val expected: String,
        val payload: Payload,
    )

    /** A fully validated key reference parsed out of a request. */
    sealed interface Request {
        fun toJson(): JsonObject
    }

    class View(
        val filter: String,
        val page: Int,
        val pageSize: Int,
        val show: String,
        val container: ContainerAddress?,
    ) : Request {
        override fun toJson(): JsonObject = buildJsonObject {
            put(ACTION, ACTION_VIEW)
            put(FILTER, filter)
            put(PAGE, page)
            put(PAGE_SIZE, pageSize)
            put(SHOW, show)
            put(CONTAINER, container?.toId().orEmpty())
        }
    }

    class EncodeInstall(val install: Install) : Request {
        override fun toJson(): JsonObject = buildJsonObject {
            put(ACTION, ACTION_ENCODE_INSTALL)
            put(CONTAINER, install.container.toId())
            put(DIMENSION, install.dimension)
            put(SLOT, install.slot)
            put(EXPECTED, install.expected)
            val payload = install.payload
            put(MODE, payload.mode)
            when (payload) {
                is Crafting -> {
                    putJsonArray(GRID) {
                        for (entry in payload.grid) add(entry?.let(::keyRefJson) ?: JsonNull)
                    }
                    payload.output?.let { put(OUTPUT, keyRefJson(it)) }
                    payload.recipe?.let { put(RECIPE, it) }
                    put(SUBSTITUTE, payload.substitute)
                    put(FLUID_SUBSTITUTE, payload.fluidSubstitute)
                }

                is Processing -> {
                    putJsonArray(INPUTS) { for (entry in payload.inputs) add(entryJson(entry)) }
                    putJsonArray(OUTPUTS) { for (entry in payload.outputs) add(entryJson(entry)) }
                }

                is Stonecutting -> {
                    put(INPUT, keyRefJson(payload.input))
                    payload.output?.let { put(OUTPUT, keyRefJson(it)) }
                    payload.recipe?.let { put(RECIPE, it) }
                    put(SUBSTITUTE, payload.substitute)
                }

                is Smithing -> {
                    put(TEMPLATE, keyRefJson(payload.template))
                    put(BASE, keyRefJson(payload.base))
                    put(ADDITION, keyRefJson(payload.addition))
                    payload.output?.let { put(OUTPUT, keyRefJson(it)) }
                    payload.recipe?.let { put(RECIPE, it) }
                    put(SUBSTITUTE, payload.substitute)
                }
            }
        }
    }

    /** The mode-specific half of an install request. */
    sealed interface Payload {
        val mode: String
    }

    class Crafting(
        val grid: List<KeyRef?>,
        val output: KeyRef?,
        val recipe: String?,
        val substitute: Boolean,
        val fluidSubstitute: Boolean,
    ) : Payload {
        override val mode: String get() = MODE_CRAFTING
    }

    class Processing(val inputs: List<StackEntry>, val outputs: List<StackEntry>) : Payload {
        override val mode: String get() = MODE_PROCESSING
    }

    class Stonecutting(
        val input: KeyRef,
        val output: KeyRef?,
        val recipe: String?,
        val substitute: Boolean,
    ) : Payload {
        override val mode: String get() = MODE_STONECUTTING
    }

    class Smithing(
        val template: KeyRef,
        val base: KeyRef,
        val addition: KeyRef,
        val output: KeyRef?,
        val recipe: String?,
        val substitute: Boolean,
    ) : Payload {
        override val mode: String get() = MODE_SMITHING
    }

    /** A container address as reported by `view`: a block position plus `block` or a vanilla part side. */
    class ContainerAddress(val x: Int, val y: Int, val z: Int, val side: String) {
        fun toId(): String = "$x,$y,$z/$side"

        override fun equals(other: Any?): Boolean =
            other is ContainerAddress && other.x == x && other.y == y && other.z == z && other.side == side

        override fun hashCode(): Int = ((x * 31 + y) * 31 + z) * 31 + side.hashCode()

        override fun toString(): String = toId()
    }

    // --- parsing --------------------------------------------------------------

    /** Parses one request; every returned value is already bounded and normalized. */
    fun parse(arguments: JsonObject): Request = when (val action = requiredString(arguments, ACTION, MAX_ID_LENGTH)) {
        ACTION_VIEW -> view(arguments)
        ACTION_ENCODE_INSTALL -> EncodeInstall(install(arguments))
        else -> throw IllegalArgumentException(
            "parameter '$ACTION' must be '${ACTION_VIEW}' or '${ACTION_ENCODE_INSTALL}'",
        )
    }

    private fun view(arguments: JsonObject): View {
        rejectUnknown(arguments, setOf(ACTION, FILTER, PAGE, PAGE_SIZE, SHOW, CONTAINER))
        return View(
            filter = optionalFilter(arguments),
            page = optionalInt(arguments, PAGE, 0, 0, Ae2ToolLimits.MAX_PAGE),
            pageSize = optionalInt(
                arguments, PAGE_SIZE, Ae2ToolLimits.DEFAULT_PAGE_SIZE, 1, Ae2ToolLimits.MAX_PAGE_SIZE,
            ),
            show = optionalShow(arguments),
            container = arguments[CONTAINER]?.let {
                val text = optionalText(it, CONTAINER, MAX_CONTAINER_ID_LENGTH)
                if (text.isEmpty()) null else parseContainerId(text)
            },
        )
    }

    /** Field names an install may carry for its mode. */
    private fun installKeys(mode: String): Set<String> = setOf(DIMENSION) + when (mode) {
        MODE_CRAFTING -> setOf(
            ACTION, CONTAINER, SLOT, EXPECTED, MODE, GRID, OUTPUT, RECIPE, SUBSTITUTE, FLUID_SUBSTITUTE,
        )

        MODE_PROCESSING -> setOf(ACTION, CONTAINER, SLOT, EXPECTED, MODE, INPUTS, OUTPUTS)
        MODE_STONECUTTING -> setOf(ACTION, CONTAINER, SLOT, EXPECTED, MODE, INPUT, OUTPUT, RECIPE, SUBSTITUTE)
        MODE_SMITHING -> setOf(
            ACTION, CONTAINER, SLOT, EXPECTED, MODE, TEMPLATE, BASE, ADDITION, OUTPUT, RECIPE, SUBSTITUTE,
        )

        else -> throw IllegalArgumentException(
            "parameter '$MODE' must be one of ${MODES.sorted().joinToString(", ")}",
        )
    }

    private fun install(arguments: JsonObject): Install {
        // The mode decides which parameters exist at all, so it is read and checked first: a parameter that
        // belongs to a different mode is then rejected as unknown instead of being silently ignored.
        val mode = requiredString(arguments, MODE, MAX_ID_LENGTH)
        rejectUnknown(arguments, installKeys(mode))

        val container = parseContainerId(requiredString(arguments, CONTAINER, MAX_CONTAINER_ID_LENGTH))
        val dimension = requiredString(arguments, DIMENSION, MAX_ID_LENGTH)
        require(isRegistryId(dimension)) { "parameter '$DIMENSION' must be a namespaced dimension id" }
        val slot = requiredInt(arguments, SLOT, 0, MAX_TARGET_SLOT)
        val expected = expected(arguments)

        val payload = when (mode) {
            MODE_CRAFTING -> Crafting(
                grid = grid(arguments),
                output = optionalKeyRef(arguments, OUTPUT),
                recipe = optionalRecipe(arguments),
                substitute = optionalBoolean(arguments, SUBSTITUTE, false),
                fluidSubstitute = optionalBoolean(arguments, FLUID_SUBSTITUTE, true),
            )

            MODE_PROCESSING -> Processing(
                inputs = stackEntries(arguments, INPUTS, MAX_PROCESSING_INPUT_SLOT).also { inputs ->
                    require(inputs.isNotEmpty()) { "parameter '$INPUTS' must hold at least one input" }
                },
                outputs = stackEntries(arguments, OUTPUTS, MAX_PROCESSING_OUTPUT_SLOT).also { outputs ->
                    require(outputs.any { it.slot == 0 }) {
                        "parameter '$OUTPUTS' must contain the primary output in slot 0"
                    }
                },
            )

            MODE_STONECUTTING -> Stonecutting(
                input = requiredKeyRef(arguments, INPUT),
                output = optionalKeyRef(arguments, OUTPUT),
                recipe = optionalRecipe(arguments),
                substitute = optionalBoolean(arguments, SUBSTITUTE, false),
            )

            MODE_SMITHING -> Smithing(
                template = requiredKeyRef(arguments, TEMPLATE),
                base = requiredKeyRef(arguments, BASE),
                addition = requiredKeyRef(arguments, ADDITION),
                output = optionalKeyRef(arguments, OUTPUT),
                recipe = optionalRecipe(arguments),
                substitute = optionalBoolean(arguments, SUBSTITUTE, false),
            )

            else -> throw IllegalArgumentException(
                "parameter '$MODE' must be one of ${MODES.sorted().joinToString(", ")}",
            )
        }

        return Install(container, dimension, slot, expected, payload)
    }

    private fun expected(arguments: JsonObject): String {
        val text = requiredString(arguments, EXPECTED, Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
        if (text == EXPECTED_EMPTY) return text
        require(Ae2ToolLimits.isItemKeyFingerprint(text)) {
            "parameter '$EXPECTED' must be '$EXPECTED_EMPTY' or the ${
                Ae2ToolLimits.KEY_FINGERPRINT_LENGTH
            } character lowercase hex pattern key the view reports for that slot"
        }
        return text
    }

    /** Exactly `MAX_CRAFTING_SLOTS` slots, each empty or one item key. */
    private fun grid(arguments: JsonObject): List<KeyRef?> {
        val element = arguments[GRID] ?: JsonArray(emptyList())
        require(element is JsonArray) { "parameter '$GRID' must be an array of at most $MAX_CRAFTING_SLOTS entries" }
        require(element.size in 1..MAX_CRAFTING_SLOTS) {
            "parameter '$GRID' must hold between 1 and $MAX_CRAFTING_SLOTS entries"
        }
        val slots = element.mapIndexed { index, entry ->
            when (entry) {
                is JsonNull -> null
                else -> keyRef(entry, "$GRID[$index]")
            }
        }
        require(slots.any { it != null }) { "parameter '$GRID' must contain at least one item" }
        return slots
    }

    /** Sparse processing entries: `[{"slot": 0, "key": ..., "amount": 500}]`. */
    private fun stackEntries(arguments: JsonObject, name: String, maxSlot: Int): List<StackEntry> {
        val element = arguments[name] ?: throw IllegalArgumentException("parameter '$name' is required")
        require(element is JsonArray) { "parameter '$name' must be an array" }
        require(element.size <= maxSlot + 1) { "parameter '$name' must hold at most ${maxSlot + 1} entries" }

        val used = HashSet<Int>(element.size)
        return element.mapIndexed { index, entry ->
            require(entry is JsonObject) { "parameter '$name[$index]' must be an object" }
            rejectUnknown(entry, setOf(SLOT, KEY, AMOUNT))
            val slot = optionalInt(entry, SLOT, index, 0, maxSlot)
            require(used.add(slot)) { "parameter '$name' names slot $slot twice" }
            StackEntry(
                slot = slot,
                key = requiredKeyRef(entry, KEY, "$name[$index].$KEY"),
                amount = optionalLong(entry, AMOUNT, 1L, 1L, Ae2ToolLimits.MAX_AMOUNT),
            )
        }
    }

    /** Parses `x,y,z/block|up|down|north|south|west|east`; the container id `view` reports. */
    fun parseContainerId(value: String): ContainerAddress {
        require(value.length <= MAX_CONTAINER_ID_LENGTH) {
            "parameter '$CONTAINER' must be at most $MAX_CONTAINER_ID_LENGTH characters"
        }
        val match = CONTAINER_ID.matchEntire(value)
            ?: throw IllegalArgumentException(
                "parameter '$CONTAINER' must be a container id from action '$ACTION_VIEW', " +
                    "such as '12,64,-3/block'",
            )
        val x = match.groupValues[1].toInt()
        val y = match.groupValues[2].toInt()
        val z = match.groupValues[3].toInt()
        require(abs(x) <= MAX_COORDINATE && abs(y) <= MAX_COORDINATE && abs(z) <= MAX_COORDINATE) {
            "parameter '$CONTAINER' is outside of the world"
        }
        return ContainerAddress(x, y, z, match.groupValues[4])
    }

    /** The one place that formats a container id, so `view` and `encode_install` cannot disagree. */
    fun containerId(x: Int, y: Int, z: Int, side: String): String = ContainerAddress(x, y, z, side).toId()

    // --- helpers --------------------------------------------------------------

    private fun keyRefJson(reference: KeyRef): JsonElement = when (reference) {
        is KeyRef.Fingerprint -> JsonPrimitive(reference.value)
        is KeyRef.RegistryId -> buildJsonObject {
            put(if (reference.fluid) FLUID else ITEM, reference.id)
        }
    }

    private fun entryJson(entry: StackEntry): JsonObject = buildJsonObject {
        put(SLOT, entry.slot)
        put(KEY, keyRefJson(entry.key))
        put(AMOUNT, entry.amount)
    }

    private fun keyRef(element: JsonElement, name: String): KeyRef = when (element) {
        is JsonPrimitive -> {
            require(element.isString) { "parameter '$name' must be a fingerprint string or an id object" }
            val text = element.content
            require(Ae2ToolLimits.isItemKeyFingerprint(text)) {
                "parameter '$name' must be the ${Ae2ToolLimits.KEY_FINGERPRINT_LENGTH} character lowercase hex " +
                    "key a previous view reported, or an object with '$ITEM' or '$FLUID'"
            }
            KeyRef.Fingerprint(text)
        }

        is JsonObject -> {
            require(element.keys.size == 1 && (element.containsKey(ITEM) || element.containsKey(FLUID))) {
                "parameter '$name' must be a fingerprint string or an object with exactly one of " +
                    "'$ITEM' or '$FLUID'"
            }
            val fluid = element.containsKey(FLUID)
            val field = if (fluid) FLUID else ITEM
            val text = optionalText(element.getValue(field), "$name.$field", MAX_ID_LENGTH)
            require(isRegistryId(text)) {
                "parameter '$name.$field' must be a namespaced registry id such as 'minecraft:stone'; " +
                    "use the key a view reported for any variant that carries NBT"
            }
            KeyRef.RegistryId(fluid, text)
        }

        else -> throw IllegalArgumentException(
            "parameter '$name' must be a fingerprint string or an object with '$ITEM' or '$FLUID'",
        )
    }

    private fun requiredKeyRef(
        arguments: JsonObject,
        name: String,
        label: String = name,
    ): KeyRef {
        val element = arguments[name] ?: throw IllegalArgumentException("parameter '$label' is required")
        return keyRef(element, label)
    }

    private fun optionalKeyRef(arguments: JsonObject, name: String): KeyRef? {
        val element = arguments[name] ?: return null
        // JSON null means "not given", which is what an optional key field such as `output` accepts.
        if (element is JsonNull) return null
        return keyRef(element, name)
    }

    private fun optionalRecipe(arguments: JsonObject): String? {
        val value = arguments[RECIPE] ?: return null
        val text = optionalText(value, RECIPE, MAX_ID_LENGTH)
        require(isRegistryId(text)) {
            "parameter '$RECIPE' must be a namespaced recipe id such as 'minecraft:oak_planks'"
        }
        return text
    }

    /** True for a syntactically valid `namespace:path` registry or recipe id. */
    fun isRegistryId(value: String): Boolean =
        value.length in 3..MAX_ID_LENGTH && REGISTRY_ID.matches(value)

    private fun optionalShow(arguments: JsonObject): String {
        val value = arguments[SHOW] ?: return SHOW_VISIBLE
        val text = optionalText(value, SHOW, MAX_ID_LENGTH)
        require(text in SHOWS) { "parameter '$SHOW' must be one of ${SHOWS.sorted().joinToString(", ")}" }
        return text
    }

    private fun optionalFilter(arguments: JsonObject): String {
        val value = arguments[FILTER] ?: return ""
        val text = optionalText(value, FILTER, Ae2ToolLimits.MAX_FILTER_LENGTH).trim()
        require(text.length <= Ae2ToolLimits.MAX_FILTER_LENGTH) {
            "parameter '$FILTER' must be at most ${Ae2ToolLimits.MAX_FILTER_LENGTH} characters"
        }
        return text
    }

    private fun rejectUnknown(arguments: JsonObject, allowed: Set<String>) {
        val unknown = arguments.keys - allowed
        require(unknown.isEmpty()) {
            "unsupported parameter(s) ${unknown.sorted().joinToString(", ")}"
        }
    }

    private fun requiredString(arguments: JsonObject, name: String, maximum: Int): String {
        val element = arguments[name] ?: throw IllegalArgumentException("parameter '$name' is required")
        val text = optionalText(element, name, maximum)
        require(text.isNotEmpty()) { "parameter '$name' must not be empty" }
        return text
    }

    private fun optionalText(element: JsonElement, name: String, maximum: Int): String {
        require(element is JsonPrimitive && element.isString) { "parameter '$name' must be a string" }
        val text = element.content
        require(text.length <= maximum) { "parameter '$name' must be at most $maximum characters" }
        return text
    }

    private fun requiredInt(arguments: JsonObject, name: String, minimum: Int, maximum: Int): Int {
        val element = arguments[name] ?: throw IllegalArgumentException("parameter '$name' is required")
        return boundedInt(element, name, minimum, maximum)
    }

    private fun optionalInt(arguments: JsonObject, name: String, fallback: Int, minimum: Int, maximum: Int): Int {
        val element = arguments[name] ?: return fallback
        return boundedInt(element, name, minimum, maximum)
    }

    private fun boundedInt(element: JsonElement, name: String, minimum: Int, maximum: Int): Int {
        require(element is JsonPrimitive && !element.isString) { "parameter '$name' must be an integer" }
        val parsed = element.intOrNull ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }

    private fun optionalLong(arguments: JsonObject, name: String, fallback: Long, minimum: Long, maximum: Long): Long {
        val element = arguments[name] ?: return fallback
        require(element is JsonPrimitive && !element.isString) { "parameter '$name' must be an integer" }
        val parsed = element.longOrNull ?: throw IllegalArgumentException("parameter '$name' must be an integer")
        require(parsed in minimum..maximum) { "parameter '$name' must be between $minimum and $maximum" }
        return parsed
    }

    private fun optionalBoolean(arguments: JsonObject, name: String, fallback: Boolean): Boolean {
        val element = arguments[name] ?: return fallback
        require(element is JsonPrimitive) { "parameter '$name' must be a boolean" }
        return element.booleanOrNull
            ?: throw IllegalArgumentException("parameter '$name' must be a boolean")
    }
}
