package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Thrown as soon as a value would exceed the remaining global materialization budget. */
class IdValueBudgetExceeded(override val message: String) : RuntimeException(message)

/**
 * Bounds the JSON that one plan would materialize. Repeated references are measured as repeated
 * serialized subtrees: sharing a [JsonElement] in memory does not make the wire smaller.
 *
 * Measurement walks the tree and stops at the first over-budget node or text, so a giant value is
 * never serialized just to discover that it is too large.
 */
object IdDslValueBudget {
    data class Measurement(val nodes: Int, val bytes: Long)

    fun measure(
        value: JsonElement,
        remainingNodes: Int,
        remainingBytes: Long,
        maxDepth: Int = IdDslLimits.MAX_TYPE_DEPTH,
    ): Measurement {
        if (remainingNodes < 0 || remainingBytes < 0) {
            throw IdValueBudgetExceeded("plan value budget is already exhausted")
        }
        val counter = Counter(remainingNodes, remainingBytes, maxDepth)
        counter.value(value, 0)
        return Measurement(counter.nodes, counter.bytes)
    }

    private class Counter(
        private val maxNodes: Int,
        private val maxBytes: Long,
        private val maxDepth: Int,
    ) {
        var nodes: Int = 0
            private set
        var bytes: Long = 0
            private set

        fun value(value: JsonElement, depth: Int) {
            if (depth > maxDepth) throw IdValueBudgetExceeded("literal nesting exceeds $maxDepth")
            addNode(1)
            when (value) {
                is JsonNull -> addBytes(4)
                is JsonPrimitive -> {
                    if (value.isString) {
                        quoted(value.content)
                    } else {
                        addBytes(value.content.length.toLong())
                    }
                }

                is JsonArray -> {
                    addBytes(2)
                    var first = true
                    for (element in value) {
                        if (!first) addBytes(1)
                        first = false
                        value(element, depth + 1)
                    }
                }

                is JsonObject -> {
                    addBytes(2)
                    var first = true
                    for ((key, entry) in value) {
                        if (!first) addBytes(1)
                        first = false
                        quoted(key)
                        addBytes(1)
                        value(entry, depth + 1)
                    }
                }
            }
        }

        private fun quoted(text: String) {
            addBytes(2)
            var index = 0
            while (index < text.length) {
                val character = text[index]
                val width = when {
                    character == '"' || character == '\\' -> 2
                    character.code < 0x20 -> when (character) {
                        '\b', '\u000C', '\n', '\r', '\t' -> 2
                        else -> 6
                    }
                    character.code < 0x80 -> 1
                    character.code < 0x800 -> 2
                    character.isHighSurrogate() && index + 1 < text.length &&
                        text[index + 1].isLowSurrogate() -> {
                        index++
                        4
                    }
                    else -> 3
                }
                addBytes(width.toLong())
                index++
            }
        }

        private fun addNode(count: Int) {
            nodes += count
            if (nodes > maxNodes) {
                throw IdValueBudgetExceeded("JSON expansion exceeds the remaining $maxNodes-node budget")
            }
        }

        private fun addBytes(count: Long) {
            bytes += count
            if (bytes > maxBytes) {
                throw IdValueBudgetExceeded(
                    "plan values exceed the ${IdDslLimits.MAX_PLAN_BYTES}-byte plan budget",
                )
            }
        }
    }
}
