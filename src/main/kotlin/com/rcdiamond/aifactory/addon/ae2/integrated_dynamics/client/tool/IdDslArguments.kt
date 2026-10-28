package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslLimits
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslWire
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdWireException
import com.rcdiamond.aifactory.client.tool.ToolArgumentException
import com.rcdiamond.aifactory.lang.utf8ByteLength
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Model-facing source and symbolic bindings; never accepts a caller-supplied native card plan. */
internal data class IdDslArguments(val source: String, val bindings: JsonObject) {
    companion object {
        const val MAX_SOURCE_BYTES = IdDslLimits.MAX_SOURCE_BYTES
        const val MAX_BINDINGS = IdDslLimits.MAX_PORTS
        private val FIELDS = setOf("source", "bindings")

        fun parse(value: JsonElement): IdDslArguments {
            val arguments = value as? JsonObject ?: invalid("DSL arguments must be a JSON object")
            if (arguments.keys.any { it !in FIELDS }) invalid("Only source and bindings are accepted")
            val source = string(arguments["source"], "source")
            if (source.length > MAX_SOURCE_BYTES || utf8ByteLength(source, MAX_SOURCE_BYTES) > MAX_SOURCE_BYTES) {
                invalid("source exceeds $MAX_SOURCE_BYTES UTF-8 bytes; split the program into smaller outputs")
            }
            val bindings = if ("bindings" in arguments) {
                arguments["bindings"] as? JsonObject ?: invalid("bindings must be a JSON object")
            } else JsonObject(emptyMap())
            if (bindings.size > MAX_BINDINGS) invalid("At most $MAX_BINDINGS input bindings are accepted")
            for ((name, element) in bindings) {
                if (!IdDslWire.isIdentifier(name)) invalid("Binding '$name' must use an ASCII DSL identifier")
                try {
                    IdDslWire.parseBinding(element)
                } catch (failure: IdWireException) {
                    invalid("bindings.$name: ${failure.message}")
                }
            }
            return IdDslArguments(source, bindings)
        }

        fun schema(): JsonObject = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") { add(JsonPrimitive("source")) }
            putJsonObject("properties") {
                putJsonObject("source") {
                    put("type", "string")
                    put("maxLength", MAX_SOURCE_BYTES)
                    put("description", "Haskell-like ID program, at most $MAX_SOURCE_BYTES UTF-8 bytes. " +
                        "Use input name : Type; let name = expression; output port = expression; with semicolons. " +
                        "No lambdas, user function definitions, recursion, coordinates or native card NBT.")
                }
                putJsonObject("bindings") {
                    put("type", "object")
                    put("maxProperties", MAX_BINDINGS)
                    put("description", "Map each input name to a real existing network variable or reader aspect. " +
                        "Do not supply types or invent IDs. Omit for programs without inputs.")
                    putJsonObject("propertyNames") { put("pattern", "^[A-Za-z_][A-Za-z0-9_]*$") }
                    putJsonObject("additionalProperties") {
                        putJsonArray("oneOf") {
                            add(buildJsonObject {
                                put("type", "object"); put("additionalProperties", false)
                                putJsonArray("required") { add(JsonPrimitive("kind")); add(JsonPrimitive("id")) }
                                putJsonObject("properties") {
                                    putJsonObject("kind") { put("const", "variable") }
                                    putJsonObject("id") { put("type", "integer"); put("minimum", 0) }
                                }
                            })
                            add(buildJsonObject {
                                put("type", "object"); put("additionalProperties", false)
                                putJsonArray("required") {
                                    add(JsonPrimitive("kind")); add(JsonPrimitive("part_id")); add(JsonPrimitive("aspect"))
                                }
                                putJsonObject("properties") {
                                    putJsonObject("kind") { put("const", "aspect") }
                                    putJsonObject("part_id") { put("type", "integer"); put("minimum", 0) }
                                    putJsonObject("aspect") {
                                        put("type", "string"); put("maxLength", IdDslLimits.MAX_LITERAL_TEXT_BYTES)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }

        private fun string(value: JsonElement?, field: String): String =
            (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: invalid("$field must be a string")

        private fun invalid(message: String): Nothing = throw ToolArgumentException(message)
    }
}
