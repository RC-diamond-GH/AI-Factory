package com.rcdiamond.aifactory.shared

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingType
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.GlobalPrompt
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * World-save JSON form of an [LLMContext].
 *
 * The context is stored as plain JSON so a session survives reloads without any provider-specific or
 * client-only type entering the server. [encode]/[decode] are exact inverses for every context this mod
 * produces (`decode(encode(context))` yields an equal context and `encode(decode(json))` a byte-identical
 * object), which is what lets the client and the server agree on [hash] without exchanging the payload.
 *
 * ## Hash, tokens and charge
 * [hash] is the SHA-256 of the *canonical* text of the object: object keys are sorted recursively, array
 * order is preserved, and strings/numbers keep their value. Two structurally equal objects therefore hash
 * identically no matter how they were built. Only the context document — never an id, title or timestamp —
 * feeds the hash, so saving an unchanged conversation is a no-op.
 *
 * [tokens] counts the canonical context with the local `cl100k_base` tokenizer. This fixed storage
 * accounting is identical on both sides and independent of the player's provider or reported API usage.
 * A cell's charge is `ceil(tokens / 32)` ME bytes ([SharedSessionLimits.chargeFor]). Other model
 * tokenizers may report different usage; that does not change the storage budget.
 *
 * ## Validation
 * [decode] rejects a payload that is nested deeper than [MAX_DEPTH] levels, larger than [MAX_CONTEXT_BYTES],
 * longer than [MAX_MESSAGES] messages, has a broken conversation (a tool result without its call, a call
 * answered twice, a user/assistant message while tool results are still owed). Rejection is an
 * [IllegalArgumentException] whose message describes the rule, never the payload.
 */
object SharedContextCodec {
    private val encoding by lazy {
        Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE)
    }
    /** Deepest nesting accepted in a context document (objects and arrays both count). */
    const val MAX_DEPTH: Int = 64

    /** Largest canonical UTF-8 size accepted for one context; a full 256Ki cell stays well under it. */
    const val MAX_CONTEXT_BYTES: Long = 64L * 1024 * 1024 - 4096

    /** Largest number of messages accepted in one conversation. */
    const val MAX_MESSAGES: Int = 200_000


    /** An empty context, the initial body of a newly created session. */
    fun empty(): JsonObject = encode(LLMContext(systemPrompt = "", messages = emptyList()))

    fun encode(context: LLMContext): JsonObject = buildJsonObject {
        put("systemPrompt", context.systemPrompt)
        put(
            "globalPrompt",
            buildJsonArray {
                context.globalPrompt.forEach { prompt ->
                    add(
                        buildJsonObject {
                            put("filename", prompt.filename)
                            put("content", prompt.content)
                        },
                    )
                }
            },
        )
        context.tools?.let { tools ->
            put(
                "tools",
                buildJsonArray {
                    tools.forEach { tool ->
                        add(
                            buildJsonObject {
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", tool.parameters)
                            },
                        )
                    }
                },
            )
        }
        put("messages", buildJsonArray { context.messages.forEach { add(encodeMessage(it)) } })
    }

    fun decode(json: JsonObject): LLMContext {
        requireWithinDepth(json, 0)
        if (canonicalText(json).toByteArray(Charsets.UTF_8).size > MAX_CONTEXT_BYTES) {
            throw IllegalArgumentException("The context is larger than $MAX_CONTEXT_BYTES bytes")
        }
        val systemPrompt = json.requireString("systemPrompt")
        val globalPrompt = (json["globalPrompt"] as? JsonArray ?: JsonArray(emptyList())).map { element ->
            val prompt = element.requireObject("globalPrompt entry")
            GlobalPrompt(prompt.requireString("filename"), prompt.requireString("content"))
        }
        val tools = when (val element = json["tools"]) {
            null, is JsonNull -> null
            is JsonArray -> element.map { decodeTool(it) }
            else -> throw IllegalArgumentException("'tools' must be an array or null")
        }
        val messageElements = json["messages"] as? JsonArray
            ?: throw IllegalArgumentException("'messages' must be an array")
        if (messageElements.size > MAX_MESSAGES) {
            throw IllegalArgumentException("The conversation has more than $MAX_MESSAGES messages")
        }
        val messages = messageElements.map { decodeMessage(it) }
        requireValidConversation(messages)
        return LLMContext(systemPrompt, globalPrompt, tools, messages)
    }

    /** Canonical-text SHA-256, lowercase hex. See the class documentation for the canonical form. */
    fun hash(json: JsonObject): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalText(json).toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(digest.size * 2)
        for (byte in digest) {
            hex.append(HEX[(byte.toInt() ushr 4) and 0xF])
            hex.append(HEX[byte.toInt() and 0xF])
        }
        return hex.toString()
    }

    /** Fixed storage token count; special-token-looking user text is counted as ordinary text. */
    fun tokens(json: JsonObject): Long = encoding.countTokensOrdinary(canonicalText(json)).toLong()

    /** The canonical text of [json]: keys sorted recursively, array order preserved. */
    fun canonicalText(json: JsonElement): String {
        val out = StringBuilder()
        writeCanonical(json, out)
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"

    private fun writeCanonical(element: JsonElement, out: StringBuilder) {
        when (element) {
            is JsonNull -> out.append("null")
            is JsonPrimitive -> if (element.isString) writeString(element.content, out) else out.append(element.content)
            is JsonArray -> {
                out.append('[')
                element.forEachIndexed { index, item ->
                    if (index > 0) out.append(',')
                    writeCanonical(item, out)
                }
                out.append(']')
            }
            is JsonObject -> {
                out.append('{')
                var first = true
                for ((key, value) in element.entries.sortedBy { it.key }) {
                    if (!first) out.append(',')
                    first = false
                    writeString(key, out)
                    out.append(':')
                    writeCanonical(value, out)
                }
                out.append('}')
            }
        }
    }

    private fun writeString(value: String, out: StringBuilder) {
        out.append('"')
        for (character in value) {
            when (character) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (character < ' ') {
                    out.append("\\u").append(character.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(character)
                }
            }
        }
        out.append('"')
    }

    private fun requireWithinDepth(element: JsonElement, depth: Int) {
        if (depth > MAX_DEPTH) {
            throw IllegalArgumentException("The context is nested deeper than $MAX_DEPTH levels")
        }
        when (element) {
            is JsonObject -> element.values.forEach { requireWithinDepth(it, depth + 1) }
            is JsonArray -> element.forEach { requireWithinDepth(it, depth + 1) }
            else -> Unit
        }
    }


    private fun encodeMessage(message: ChatMessage): JsonObject = when (message) {
        is ChatMessage.User -> buildJsonObject {
            put("role", "user")
            put("content", message.content)
        }
        is ChatMessage.Assistant -> buildJsonObject {
            put("role", "assistant")
            message.content?.let { put("content", it) }
            put(
                "toolCalls",
                buildJsonArray {
                    message.toolCalls.forEach { call ->
                        add(
                            buildJsonObject {
                                put("id", call.id)
                                put("name", call.name)
                                put("arguments", call.arguments)
                            },
                        )
                    }
                },
            )
        }
        is ChatMessage.Tool -> buildJsonObject {
            put("role", "tool")
            put("toolCallId", message.toolCallId)
            put("content", message.content)
        }
    }

    private fun decodeMessage(element: JsonElement): ChatMessage {
        val message = element.requireObject("message")
        return when (val role = message.requireString("role")) {
            "user" -> ChatMessage.User(message.requireString("content"))
            "assistant" -> {
                val calls = (message["toolCalls"] as? JsonArray ?: JsonArray(emptyList())).map { decodeToolCall(it) }
                ChatMessage.Assistant(message.stringOrNull("content"), calls)
            }
            "tool" -> ChatMessage.Tool(message.requireString("toolCallId"), message.requireString("content"))
            else -> throw IllegalArgumentException("Unknown message role '$role'")
        }
    }

    private fun decodeTool(element: JsonElement): ToolDefinition {
        val tool = element.requireObject("tool")
        val name = tool.requireString("name")
        val parameters = tool["parameters"]
            ?: throw IllegalArgumentException("Tool '$name' has no parameters")
        return ToolDefinition(name, tool.requireString("description"), parameters)
    }

    private fun decodeToolCall(element: JsonElement): ToolCall {
        val call = element.requireObject("tool call")
        val arguments = call["arguments"] ?: JsonObject(emptyMap())
        return ToolCall(call.requireString("id"), call.requireString("name"), arguments)
    }

    /**
     * Enforces a conversation the server can hand back to a provider unchanged: every tool result answers
     * an outstanding call, no call is answered twice, and no user/assistant message is appended while tool
     * results are still owed. A trailing assistant message whose calls are not yet answered is allowed.
     */
    private fun requireValidConversation(messages: List<ChatMessage>) {
        val pending = LinkedHashSet<String>()
        messages.forEachIndexed { index, message ->
            when (message) {
                is ChatMessage.User -> if (pending.isNotEmpty()) {
                    throw IllegalArgumentException("Message $index: a user message follows unanswered tool calls")
                }
                is ChatMessage.Assistant -> {
                    if (pending.isNotEmpty()) {
                        throw IllegalArgumentException("Message $index: an assistant message follows unanswered tool calls")
                    }
                    message.toolCalls.forEach { call ->
                        if (call.id.isEmpty()) throw IllegalArgumentException("Message $index: a tool call has no id")
                        if (call.name.isEmpty()) throw IllegalArgumentException("Message $index: a tool call has no name")
                        if (!pending.add(call.id)) {
                            throw IllegalArgumentException("Message $index: duplicate tool call id '${call.id}'")
                        }
                    }
                }
                is ChatMessage.Tool -> {
                    if (message.toolCallId.isEmpty()) {
                        throw IllegalArgumentException("Message $index: a tool result has no call id")
                    }
                    if (!pending.remove(message.toolCallId)) {
                        throw IllegalArgumentException("Message $index: tool result '${message.toolCallId}' has no matching call")
                    }
                }
            }
        }
    }
}

/** Reads a field that must be present and be a JSON string. */
internal fun JsonObject.requireString(field: String): String {
    val element = this[field] ?: throw IllegalArgumentException("Missing field '$field'")
    val primitive = element as? JsonPrimitive ?: throw IllegalArgumentException("Field '$field' must be a string")
    if (!primitive.isString) throw IllegalArgumentException("Field '$field' must be a string")
    return primitive.content
}

/** Reads a field that must be present and be a JSON boolean. */
internal fun JsonObject.requireBoolean(field: String): Boolean {
    val element = this[field] ?: throw IllegalArgumentException("Missing field '$field'")
    val primitive = element as? JsonPrimitive ?: throw IllegalArgumentException("Field '$field' must be a boolean")
    return primitive.booleanOrNull ?: throw IllegalArgumentException("Field '$field' must be a boolean")
}

/** Reads a field that must be present and be a JSON integer. */
internal fun JsonObject.requireInt(field: String): Int {
    val element = this[field] ?: throw IllegalArgumentException("Missing field '$field'")
    val primitive = element as? JsonPrimitive ?: throw IllegalArgumentException("Field '$field' must be an integer")
    return primitive.intOrNull ?: throw IllegalArgumentException("Field '$field' must be an integer")
}

/** Reads a field that must be present and be a UUID string. */
internal fun JsonObject.requireUuid(field: String): UUID {
    val text = requireString(field)
    return try {
        UUID.fromString(text)
    } catch (invalid: IllegalArgumentException) {
        throw IllegalArgumentException("Field '$field' is not a valid UUID")
    }
}

/** Reads an optional string field: absent or `null` yields `null`, any other non-string fails. */
internal fun JsonObject.stringOrNull(field: String): String? = when (val element = this[field]) {
    null, is JsonNull -> null
    is JsonPrimitive -> if (element.isString) element.content else throw IllegalArgumentException("Field '$field' must be a string")
    else -> throw IllegalArgumentException("Field '$field' must be a string")
}

private fun JsonElement?.requireObject(what: String): JsonObject =
    this as? JsonObject ?: throw IllegalArgumentException("Each $what must be an object")
