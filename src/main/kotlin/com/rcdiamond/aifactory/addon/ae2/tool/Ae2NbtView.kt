package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/**
 * A tag tree independent of Minecraft's NBT classes, so canonical fingerprints and bounded rendering
 * are pure functions that can be unit tested without booting the game.
 *
 * The engine-side adapter converts `net.minecraft.nbt.Tag` into this model exactly once per request;
 * after that every decision (fingerprint bytes, navigation, budget) is deterministic and free of
 * engine types.
 */
sealed interface Ae2Tag {
    /**
     * Compound members. The adapter stores them sorted by name so iteration, rendering and
     * fingerprinting are stable regardless of the NBT implementation's internal map order.
     */
    data class Compound(val entries: Map<String, Ae2Tag>) : Ae2Tag

    /** List with the vanilla element type id. */
    data class Sequence(val elements: List<Ae2Tag>, val elementType: Int) : Ae2Tag

    /** Byte, short, int or long; [type] is the vanilla tag id. */
    data class Signed(val type: Int, val value: Long) : Ae2Tag

    /** Float or double kept as raw IEEE bits so canonical bytes are exact. */
    data class Decimal(val type: Int, val bits: Long) : Ae2Tag

    data class Text(val value: String) : Ae2Tag

    class Bytes(val values: ByteArray) : Ae2Tag
    class Ints(val values: IntArray) : Ae2Tag
    class Longs(val values: LongArray) : Ae2Tag
}

/** Vanilla NBT type ids, used for both the canonical encoding and the reported node kind. */
object Ae2TagType {
    const val BYTE = 1
    const val SHORT = 2
    const val INT = 3
    const val LONG = 4
    const val FLOAT = 5
    const val DOUBLE = 6
    const val BYTE_ARRAY = 7
    const val STRING = 8
    const val LIST = 9
    const val COMPOUND = 10
    const val INT_ARRAY = 11
    const val LONG_ARRAY = 12
}

/**
 * Canonical encoding, fingerprinting, path navigation and bounded JSON rendering for [Ae2Tag]s.
 *
 * ## Fingerprints
 * [fingerprint] hashes the AE key's channel id and its canonical NBT bytes. Keys are sorted, numbers
 * are big-endian, floats and doubles keep their raw bits and strings are UTF-8 length prefixed, so the
 * same logical key always produces the same fingerprint across JVM runs and server restarts. The
 * identifier is what the model hands back to [Ae2ToolLimits.OPERATION_ITEM_DETAILS] and
 * [Ae2ToolLimits.OPERATION_CRAFTING_PLAN]; it covers the full key including its NBT, so two variants of
 * one item never collide.
 *
 * ## Bounded rendering
 * Rendering never emits a full tag tree. Nesting is cut at `depth`, at most
 * [Ae2ToolLimits.MAX_RENDERED_NODES] nodes are emitted in total, strings are shortened and arrays are
 * previewed. Whenever something was left out the container becomes a self-describing object such as
 * `{"_type":"compound","_shown":20,"_total":57,"_entries":{...}}`; complete containers stay plain JSON
 * objects and arrays. The top-level children of the rendered node are paged by the caller.
 */
object Ae2NbtView {
    /** One navigation step: a compound member or a list element. */
    sealed interface Step {
        data class Member(val name: String) : Step
        data class Element(val index: Int) : Step
    }

    /** Result of navigating from a root tag. */
    sealed interface Navigation {
        data class Found(val node: Ae2Tag) : Navigation

        /** The path left the tree; [failedAtStep] is the zero based step that did not resolve. */
        data class Missing(val failedAtStep: Int, val stepCount: Int) : Navigation
    }

    /** A rendered node plus the metadata the response needs to describe what was shown. */
    class Rendered(
        val value: JsonElement,
        val kind: String,
        val childTotal: Int,
        val childShown: Int,
        val truncated: Boolean,
    )

    private class Budget(private var remaining: Int) {
        fun hasCapacity(): Boolean = remaining > 0

        fun take(): Boolean {
            if (remaining <= 0) return false
            remaining--
            return true
        }
    }

    /**
     * Parses a dotted navigation path such as `tag.display.Name` or `tag.Items[0].id`.
     *
     * `\.` and `\\` escape a literal dot and backslash inside a member name; `[n]` addresses a list
     * element with a zero based index; an empty path addresses the root. Invalid input produces an
     * [IllegalArgumentException] that names the position but never echoes the model's text.
     */
    fun parsePath(path: String): List<Step> {
        require(path.length <= Ae2ToolLimits.MAX_PATH_LENGTH) {
            "path must be at most ${Ae2ToolLimits.MAX_PATH_LENGTH} characters"
        }
        if (path.isEmpty()) return emptyList()

        val steps = ArrayList<Step>()
        val name = StringBuilder()

        fun push(step: Step) {
            steps += step
            require(steps.size <= Ae2ToolLimits.MAX_PATH_STEPS) {
                "path must have at most ${Ae2ToolLimits.MAX_PATH_STEPS} steps"
            }
        }

        fun flushName(required: Boolean) {
            if (name.isEmpty()) {
                require(!required) { "path segment ${steps.size + 1} is empty" }
                return
            }
            require(name.length <= Ae2ToolLimits.MAX_PATH_SEGMENT_LENGTH) {
                "path segment ${steps.size + 1} must be at most ${Ae2ToolLimits.MAX_PATH_SEGMENT_LENGTH} characters"
            }
            push(Step.Member(name.toString()))
            name.setLength(0)
        }

        var index = 0
        while (index < path.length) {
            when (val character = path[index]) {
                '\\' -> {
                    require(index + 1 < path.length) { "path ends with an escape character" }
                    name.append(path[index + 1])
                    index += 2
                }

                '.' -> {
                    flushName(required = true)
                    index++
                    require(index < path.length) { "path must not end with a separator" }
                }

                '[' -> {
                    flushName(required = false)
                    val close = path.indexOf(']', index + 1)
                    require(close > index + 1) { "path element ${steps.size + 1} is malformed" }
                    val digits = path.substring(index + 1, close)
                    require(digits.all { it in '0'..'9' }) {
                        "path element ${steps.size + 1} must use a non-negative index"
                    }
                    val element = digits.toIntOrNull()
                        ?: throw IllegalArgumentException("path element index is out of range")
                    require(element <= Ae2ToolLimits.MAX_PATH_INDEX) {
                        "path element index must be at most ${Ae2ToolLimits.MAX_PATH_INDEX}"
                    }
                    push(Step.Element(element))
                    index = close + 1
                    if (index < path.length) {
                        require(path[index] == '.') { "path must separate steps with '.'" }
                        index++
                        require(index < path.length) { "path must not end with a separator" }
                    }
                }

                ']' -> throw IllegalArgumentException("path contains an unmatched ']'")

                else -> {
                    name.append(character)
                    index++
                }
            }
        }
        flushName(required = false)
        return steps
    }

    /** Follows [steps] from [root]. */
    fun navigate(root: Ae2Tag, steps: List<Step>): Navigation {
        var node: Ae2Tag = root
        for ((position, step) in steps.withIndex()) {
            node = when (step) {
                is Step.Member -> (node as? Ae2Tag.Compound)?.entries?.get(step.name)
                    ?: return Navigation.Missing(position, steps.size)

                is Step.Element -> (node as? Ae2Tag.Sequence)?.elements?.getOrNull(step.index)
                    ?: return Navigation.Missing(position, steps.size)
            }
        }
        return Navigation.Found(node)
    }

    /** Renders one node, paging the direct children of the node itself when it is a container. */
    fun render(node: Ae2Tag, depth: Int, page: Int, pageSize: Int): Rendered =
        renderNode(node, depth, page, pageSize, Budget(Ae2ToolLimits.MAX_RENDERED_NODES))

    /** Stable name of a tag's kind, as reported in responses. */
    fun kindOf(tag: Ae2Tag): String = when (tag) {
        is Ae2Tag.Compound -> "compound"
        is Ae2Tag.Sequence -> "list"
        is Ae2Tag.Signed -> signedKind(tag.type)
        is Ae2Tag.Decimal -> if (tag.type == Ae2TagType.FLOAT) "float" else "double"
        is Ae2Tag.Text -> "string"
        is Ae2Tag.Bytes -> "byte_array"
        is Ae2Tag.Ints -> "int_array"
        is Ae2Tag.Longs -> "long_array"
    }

    /**
     * Canonical byte form of [tag]: sorted compound members, big-endian numbers, raw float bits and
     * UTF-8 length prefixed text. Two logically equal trees always encode to identical bytes.
     */
    fun canonicalBytes(tag: Ae2Tag): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { writeNode(it, tag) }
        return bytes.toByteArray()
    }

    /**
     * Lowercase hex SHA-256 of the channel id and the canonical key bytes. This is the stable item
     * identity handed to the model.
     */
    fun fingerprint(typeId: String, tag: Ae2Tag): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(typeId.toByteArray(Charsets.UTF_8))
        digest.update(0.toByte())
        digest.update(canonicalBytes(tag))
        return digest.digest().joinToString("") { byte ->
            val value = byte.toInt() and 0xFF
            HEX[value ushr 4].toString() + HEX[value and 0x0F]
        }
    }

    private const val HEX = "0123456789abcdef"

    private fun renderNode(node: Ae2Tag, depth: Int, page: Int, pageSize: Int, budget: Budget): Rendered {
        if (!budget.take()) return omitted()
        return when (node) {
            is Ae2Tag.Compound -> {
                val keys = node.entries.keys.sorted()
                renderContainer(
                    total = keys.size,
                    depth = depth,
                    page = page,
                    pageSize = pageSize,
                    budget = budget,
                    typeName = "compound",
                    nameAt = { position -> keys[position] },
                    childAt = { position -> node.entries.getValue(keys[position]) },
                )
            }

            is Ae2Tag.Sequence -> renderContainer(
                total = node.elements.size,
                depth = depth,
                page = page,
                pageSize = pageSize,
                budget = budget,
                typeName = "list",
                nameAt = null,
                childAt = { position -> node.elements[position] },
            )

            is Ae2Tag.Signed -> Rendered(JsonPrimitive(node.value), signedKind(node.type), 0, 0, false)

            is Ae2Tag.Decimal -> {
                val value: Number = if (node.type == Ae2TagType.FLOAT) {
                    java.lang.Float.valueOf(java.lang.Float.intBitsToFloat(node.bits.toInt()))
                } else {
                    java.lang.Double.valueOf(java.lang.Double.longBitsToDouble(node.bits))
                }
                val finite = value.toDouble().isFinite()
                // Non-finite values keep their name as text: JSON has no NaN or infinity literal.
                val rendered = if (finite) JsonPrimitive(value) else JsonPrimitive(value.toString())
                Rendered(rendered, kindOf(node), 0, 0, false)
            }

            is Ae2Tag.Text -> {
                val truncated = node.value.length > Ae2ToolLimits.MAX_RENDERED_STRING_LENGTH
                if (!truncated) {
                    Rendered(JsonPrimitive(node.value), "string", 0, 0, false)
                } else {
                    Rendered(
                        buildJsonObject {
                            put("_type", "string")
                            put("_length", node.value.length)
                            put("_value", node.value.take(Ae2ToolLimits.MAX_RENDERED_STRING_LENGTH))
                        },
                        "string",
                        0,
                        0,
                        true,
                    )
                }
            }

            is Ae2Tag.Bytes -> renderArray("byte_array", node.values.size, budget) { position -> node.values[position].toLong() }

            is Ae2Tag.Ints -> renderArray("int_array", node.values.size, budget) { position -> node.values[position].toLong() }

            is Ae2Tag.Longs -> renderArray("long_array", node.values.size, budget) { position -> node.values[position] }
        }
    }

    /** Renders up to `pageSize` direct children of a container; only the caller's node is paged. */
    private fun renderContainer(
        total: Int,
        depth: Int,
        page: Int,
        pageSize: Int,
        budget: Budget,
        typeName: String,
        nameAt: ((Int) -> String)?,
        childAt: (Int) -> Ae2Tag,
    ): Rendered {
        if (depth <= 0) {
            return Rendered(
                buildJsonObject {
                    put("_type", typeName)
                    put("_size", total)
                },
                typeName,
                0,
                0,
                total > 0,
            )
        }

        val from = (page.toLong() * pageSize.toLong()).coerceAtMost(total.toLong()).toInt()
        val to = (from.toLong() + pageSize.toLong()).coerceAtMost(total.toLong()).toInt()
        val available = to - from

        var truncated = available < total
        var shown = 0
        val values = LinkedHashMap<String, JsonElement>()
        while (shown < available) {
            if (!budget.hasCapacity()) {
                truncated = true
                break
            }
            val rendered = renderNode(childAt(from + shown), depth - 1, 0, Int.MAX_VALUE, budget)
            if (rendered.truncated) truncated = true
            val name = if (nameAt != null) nameAt(from + shown) else (from + shown).toString()
            values[name] = rendered.value
            shown++
        }

        if (shown == total) {
            val plain: JsonElement = if (nameAt != null) JsonObject(values) else JsonArray(values.values.toList())
            return Rendered(plain, typeName, total, shown, truncated)
        }

        val content: JsonElement = if (nameAt != null) JsonObject(values) else JsonArray(values.values.toList())
        val wrapped = buildJsonObject {
            put("_type", typeName)
            put("_shown", shown)
            put("_total", total)
            put(if (nameAt != null) "_entries" else "_items", content)
        }
        return Rendered(wrapped, typeName, total, shown, true)
    }

    private inline fun renderArray(typeName: String, length: Int, budget: Budget, value: (Int) -> Long): Rendered {
        val limit = minOf(length, Ae2ToolLimits.MAX_ARRAY_PREVIEW)
        val elements = buildJsonArray {
            var position = 0
            while (position < limit) {
                if (!budget.hasCapacity()) break
                add(JsonPrimitive(value(position)))
                position++
            }
        }
        return Rendered(
            buildJsonObject {
                put("_type", typeName)
                put("_length", length)
                put("_values", elements)
            },
            typeName,
            0,
            0,
            elements.size < length,
        )
    }

    private fun omitted(): Rendered = Rendered(
        buildJsonObject {
            put("_type", "omitted")
            put("_reason", "node_limit")
        },
        "omitted",
        0,
        0,
        true,
    )

    private fun signedKind(type: Int): String = when (type) {
        Ae2TagType.BYTE -> "byte"
        Ae2TagType.SHORT -> "short"
        Ae2TagType.INT -> "int"
        else -> "long"
    }

    private fun writeNode(out: DataOutputStream, tag: Ae2Tag) {
        when (tag) {
            is Ae2Tag.Compound -> {
                out.writeByte(Ae2TagType.COMPOUND)
                val keys = tag.entries.keys.sorted()
                out.writeInt(keys.size)
                for (key in keys) {
                    writeText(out, key)
                    writeNode(out, tag.entries.getValue(key))
                }
            }

            is Ae2Tag.Sequence -> {
                out.writeByte(Ae2TagType.LIST)
                out.writeByte(tag.elementType)
                out.writeInt(tag.elements.size)
                for (element in tag.elements) writeNode(out, element)
            }

            is Ae2Tag.Signed -> {
                out.writeByte(tag.type)
                when (tag.type) {
                    Ae2TagType.BYTE -> out.writeByte(tag.value.toInt())
                    Ae2TagType.SHORT -> out.writeShort(tag.value.toInt())
                    Ae2TagType.INT -> out.writeInt(tag.value.toInt())
                    else -> out.writeLong(tag.value)
                }
            }

            is Ae2Tag.Decimal -> {
                out.writeByte(tag.type)
                if (tag.type == Ae2TagType.FLOAT) out.writeInt(tag.bits.toInt()) else out.writeLong(tag.bits)
            }

            is Ae2Tag.Text -> {
                out.writeByte(Ae2TagType.STRING)
                writeText(out, tag.value)
            }

            is Ae2Tag.Bytes -> {
                out.writeByte(Ae2TagType.BYTE_ARRAY)
                out.writeInt(tag.values.size)
                out.write(tag.values)
            }

            is Ae2Tag.Ints -> {
                out.writeByte(Ae2TagType.INT_ARRAY)
                out.writeInt(tag.values.size)
                for (value in tag.values) out.writeInt(value)
            }

            is Ae2Tag.Longs -> {
                out.writeByte(Ae2TagType.LONG_ARRAY)
                out.writeInt(tag.values.size)
                for (value in tag.values) out.writeLong(value)
            }
        }
    }

    private fun writeText(out: DataOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
    }
}
