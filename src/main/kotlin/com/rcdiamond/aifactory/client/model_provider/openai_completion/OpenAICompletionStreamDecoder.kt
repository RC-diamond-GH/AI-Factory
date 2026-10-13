package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.FinishReason
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Incremental decoder for Chat Completions SSE data payloads, in arrival order.
 * The transport handles framing and `[DONE]`, then checks completion through [finish].
 * Only choice index 0 is consumed; other choices cannot terminate this stream.
 *
 * Text and tool-call fragments are emitted as they arrive; nothing is buffered until the end.
 * Tool arguments stay raw string fragments because a single fragment can be an incomplete JSON
 * value. Protocol violations raise [OpenAIProtocolException] with a message that states the broken
 * rule and never repeats upstream payload text.
 */
internal class OpenAICompletionStreamDecoder {
    private var finishReason: FinishReason? = null

    /** Decodes one chunk and returns the events it produced, in arrival order. */
    fun decode(data: String): List<AssistantEvent> {
        val chunk = parseJson(data, "stream chunk").objectAt("stream chunk")
        val error = chunk["error"]
        if (error != null && error !is JsonNull) invalidProtocol("stream reported an error")
        val choices = chunk["choices"] as? JsonArray ?: invalidProtocol("choices must be an array")
        val events = ArrayList<AssistantEvent>(2)
        var observed: FinishReason? = null
        var sawFirstChoice = false
        for (element in choices) {
            val choice = element.objectAt("choice")
            if (choice.intAt("index") != 0) continue
            if (sawFirstChoice) invalidProtocol("choice index 0 must appear once")
            sawFirstChoice = true
            val delta = choice["delta"]
            if (delta != null && delta !is JsonNull) {
                val fields = delta.objectAt("delta")
                if (!fields.isEmpty() && finishReason != null) {
                    invalidProtocol("stream carries data after finish_reason")
                }
                val role = fields.stringOrNullAt("role")
                if (role != null && role != "assistant") invalidProtocol("stream must have assistant role")
                // Role-only and empty deltas carry no text; they must not become empty events.
                val text = fields.stringOrNullAt("content")
                if (!text.isNullOrEmpty()) events += AssistantEvent.TextDelta(text)
                val toolCalls = fields["tool_calls"]
                if (toolCalls != null && toolCalls !is JsonNull) {
                    val calls = toolCalls as? JsonArray ?: invalidProtocol("tool_calls must be an array")
                    for (call in calls) {
                        events += toolCallDelta(call.objectAt("tool call"))
                    }
                }
            }
            val reason = choice.stringOrNullAt("finish_reason")
            if (reason != null) {
                if (finishReason != null || observed != null) invalidProtocol("finish_reason must be reported once")
                observed = finishReasonAt(reason)
            }
        }
        val usage = usageAt(chunk["usage"])
        if (observed != null) events += AssistantEvent.Finished(observed)
        if (usage != null) events += AssistantEvent.TokenUsage(usage)
        finishReason = observed ?: finishReason
        return events
    }

    /**
     * Completes the stream. A stream must report a `finish_reason` before the transport's `[DONE]`,
     * otherwise a truncated answer is indistinguishable from a completed one.
     */
    fun finish() {
        if (finishReason == null) invalidProtocol("stream ended before finish_reason")
    }

    private fun toolCallDelta(call: JsonObject): AssistantEvent.ToolCallDelta {
        val index = call.intAt("index")
        if (index < 0) invalidProtocol("tool call index must not be negative")
        val type = call.stringOrNullAt("type")
        if (type != null && type != "function") invalidProtocol("unsupported tool call type")
        val id = call.stringOrNullAt("id")
        val function = call["function"]
        if (function == null || function is JsonNull) return AssistantEvent.ToolCallDelta(index, id)
        val fields = function.objectAt("function")
        return AssistantEvent.ToolCallDelta(
            index,
            id,
            fields.stringOrNullAt("name"),
            fields.stringOrNullAt("arguments"),
        )
    }
}
