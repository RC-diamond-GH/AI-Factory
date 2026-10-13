package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.GenerateResponse
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.Usage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Request encoder and non-streaming response decoder for Chat Completions. */
internal object OpenAICompletionCodec {
    /**
     * Encodes a request. Streaming requests ask for the terminal usage chunk through
     * `stream_options`; non-streaming requests must not send that option.
     */
    fun encode(model: String, prompt: LLMContext, stream: Boolean = false): String = buildJsonObject {
        put("model", model)
        put("stream", stream)
        if (stream) {
            put("stream_options", buildJsonObject { put("include_usage", true) })
        }
        put("messages", buildJsonArray {
            add(message("system", prompt.systemPrompt))
            for (global in prompt.globalPrompt) {
                add(message("system", "${global.filename}\n${global.content}"))
            }
            for (entry in prompt.messages) {
                add(when (entry) {
                    is ChatMessage.User -> message("user", entry.content)
                    is ChatMessage.Tool -> buildJsonObject {
                        put("role", "tool")
                        put("tool_call_id", entry.toolCallId)
                        put("content", entry.content)
                    }
                    is ChatMessage.Assistant -> buildJsonObject {
                        put("role", "assistant")
                        put("content", entry.content?.let(::JsonPrimitive) ?: JsonNull)
                        if (entry.toolCalls.isNotEmpty()) {
                            put("tool_calls", buildJsonArray {
                                for (call in entry.toolCalls) {
                                    add(buildJsonObject {
                                        put("id", call.id)
                                        put("type", "function")
                                        put("function", buildJsonObject {
                                            put("name", call.name)
                                            put("arguments", call.arguments.toString())
                                        })
                                    })
                                }
                            })
                        }
                    }
                })
            }
        })
        if (!prompt.tools.isNullOrEmpty()) {
            put("tools", buildJsonArray {
                for (tool in prompt.tools) {
                    add(buildJsonObject {
                        put("type", "function")
                        put("function", buildJsonObject {
                            put("name", tool.name)
                            put("description", tool.description)
                            put("parameters", tool.parameters)
                        })
                    })
                }
            })
        }
    }.toString()

    fun decode(body: String): GenerateResponse {
        val root = parseJson(body, "response").objectAt("response")
        val choices = root["choices"] as? JsonArray ?: invalidProtocol("choices must be an array")
        val choice = choices.firstOrNull()?.objectAt("choice") ?: invalidProtocol("choices must not be empty")
        val message = choice["message"].objectAt("message")
        if (message.stringAt("role") != "assistant") invalidProtocol("response message must have assistant role")
        val content = when (val value = message["content"]) {
            null, JsonNull -> null
            is JsonPrimitive ->
                if (value.isString) value.content else invalidProtocol("content must be a string or null")
            else -> invalidProtocol("content must be a string or null")
        }
        val calls = when (val value = message["tool_calls"]) {
            null, JsonNull -> emptyList()
            is JsonArray -> value.map { element ->
                val call = element.objectAt("tool call")
                if (call.stringAt("type") != "function") invalidProtocol("unsupported tool call type")
                val function = call["function"].objectAt("function")
                ToolCall(
                    call.stringAt("id"),
                    function.stringAt("name"),
                    parseJson(function.stringAt("arguments"), "tool arguments"),
                )
            }
            else -> invalidProtocol("tool_calls must be an array or null")
        }
        val reason = finishReasonAt(choice.stringAt("finish_reason"))
        return GenerateResponse(ChatMessage.Assistant(content, calls), reason, usageAt(root["usage"]))
    }

    private fun message(role: String, content: String): JsonObject = buildJsonObject {
        put("role", role)
        put("content", content)
    }
}

/** Parses JSON text; failures never repeat the payload in the error message. */
internal fun parseJson(text: String, field: String): JsonElement = try {
    Json.parseToJsonElement(text)
} catch (_: SerializationException) {
    invalidProtocol("$field must contain valid JSON")
} catch (_: IllegalArgumentException) {
    invalidProtocol("$field must contain valid JSON")
}

/** Returns the receiver as an object, or fails when the value is absent or of another type. */
internal fun JsonElement?.objectAt(field: String): JsonObject =
    this as? JsonObject ?: invalidProtocol("$field must be an object")

/** Reads a field that must be present and be a JSON string. */
internal fun JsonObject.stringAt(field: String): String {
    val value = this[field] as? JsonPrimitive ?: invalidProtocol("$field must be a string")
    if (!value.isString) invalidProtocol("$field must be a string")
    return value.content
}

/** Reads an optional field: absent or `null` yields `null`, any other non-string fails. */
internal fun JsonObject.stringOrNullAt(field: String): String? {
    val value = this[field] ?: return null
    if (value is JsonNull) return null
    val primitive = value as? JsonPrimitive ?: invalidProtocol("$field must be a string")
    if (!primitive.isString) invalidProtocol("$field must be a string")
    return primitive.content
}

/** Reads a required integer field. */
internal fun JsonObject.intAt(field: String): Int {
    val value = this[field] as? JsonPrimitive ?: invalidProtocol("$field must be an integer")
    if (value.isString) invalidProtocol("$field must be an integer")
    return value.content.toIntOrNull() ?: invalidProtocol("$field must be an integer")
}

/** Reads a token count; unsigned and never truncated. */
internal fun JsonObject.tokensAt(field: String): ULong {
    val value = this[field] as? JsonPrimitive ?: invalidProtocol("$field must be an unsigned integer")
    if (value.isString) invalidProtocol("$field must be an unsigned integer")
    return value.content.toULongOrNull() ?: invalidProtocol("$field must be an unsigned integer")
}

/** Maps the protocol's `finish_reason`; unknown values are preserved instead of guessed. */
internal fun finishReasonAt(value: String): FinishReason = when (value) {
    "stop" -> FinishReason.Stop
    "tool_calls" -> FinishReason.ToolCalls
    "length" -> FinishReason.Length
    "content_filter" -> FinishReason.ContentFilter
    "error" -> FinishReason.Error
    else -> FinishReason.Unknown(value)
}

/** Reads the optional usage object shared by streaming and non-streaming responses. */
internal fun usageAt(value: JsonElement?): Usage? = when (value) {
    null, JsonNull -> null
    else -> value.objectAt("usage").let {
        Usage(it.tokensAt("prompt_tokens"), it.tokensAt("completion_tokens"), it.tokensAt("total_tokens"))
    }
}

/** Sanitized protocol failure: the message describes the violated rule, not the payload. */
internal fun invalidProtocol(message: String): Nothing = throw OpenAIProtocolException(message)
