package com.rcdiamond.aifactory.client.llm

import kotlinx.coroutines.flow.Flow
import java.io.IOException
import kotlinx.serialization.json.JsonElement

/** Provider-independent input. Messages and global prompts retain their order. */
data class LLMContext(
    val systemPrompt: String,
    val globalPrompt: List<GlobalPrompt> = emptyList(),
    val tools: List<ToolDefinition>? = null,
    val messages: List<ChatMessage>,
)

data class GlobalPrompt(val filename: String, val content: String)

data class ToolDefinition(val name: String, val description: String, val parameters: JsonElement)

sealed interface ChatMessage {
    data class User(val content: String) : ChatMessage
    data class Assistant(val content: String? = null, val toolCalls: List<ToolCall> = emptyList()) : ChatMessage
    data class Tool(val toolCallId: String, val content: String) : ChatMessage
}

data class ToolCall(val id: String, val name: String, val arguments: JsonElement)

data class GenerateResponse(
    val message: ChatMessage,
    val finishReason: FinishReason,
    val usage: Usage? = null,
)

data class Usage(val inputTokens: ULong, val outputTokens: ULong, val totalTokens: ULong)

sealed interface FinishReason {
    data object Stop : FinishReason
    data object ToolCalls : FinishReason
    data object Length : FinishReason
    data object ContentFilter : FinishReason
    data object Error : FinishReason
    data class Unknown(val value: String) : FinishReason
}

interface ModelProvider {
    /** Suspends without blocking the caller thread. Cancellation cancels the in-flight request. */
    suspend fun generate(prompt: LLMContext): GenerateResponse

    /**
     * Cold stream: each collection starts a request; cancellation releases its HTTP subscription.
     * Setup failures use the outer Result, API failures use a terminal failed stream item.
     * Failure values are LLMAPIError; coroutine cancellation is never converted to a failure.
     */
    suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>>
}

sealed interface AssistantEvent {
    data class TextDelta(val text: String) : AssistantEvent
    /** Arguments are raw JSON fragments, not independently parseable JSON values. */
    data class ToolCallDelta(
        val index: Int,
        val id: String? = null,
        val name: String? = null,
        val arguments: String? = null,
    ) : AssistantEvent
    data class Finished(val reason: FinishReason) : AssistantEvent
    data class TokenUsage(val usage: Usage) : AssistantEvent
}

/** Sanitized API failure: never includes credentials, prompts or upstream response bodies. */
abstract class LLMAPIError(message: String) : IOException(message)

class LLMTransportError : LLMAPIError("LLM API transport failed")
