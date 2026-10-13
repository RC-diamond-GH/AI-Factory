package com.rcdiamond.aifactory.client.agent

import com.rcdiamond.aifactory.client.llm.AgentSettings
import com.rcdiamond.aifactory.client.llm.AgentUpdate
import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ModelProvider
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.tool.ToolManager
import com.rcdiamond.aifactory.client.tool.toolFailureObservation
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.TreeMap

/**
 * A turn ended without a usable final answer: the stream was malformed, the provider reported a
 * reason the agent cannot act on, or the step budget ran out.
 *
 * Sanitized like every [LLMAPIError], and the conversation history is left protocol-valid so the
 * next user message can be sent as-is.
 */
class AgentTurnException(message: String) : LLMAPIError(message)

/**
 * Client-side ReAct loop over a streaming [ModelProvider].
 *
 * One [respond] call appends the user message, then repeats: request a model step, forward streamed
 * text, append the assistant message, execute every streamed tool call in order, append exactly one
 * [ChatMessage.Tool] reply per call, and feed those observations back into the next request until the
 * model finishes with `stop`/`length` or the step budget is exhausted. There are no automatic
 * retries.
 *
 * The agent depends only on provider-independent schema, [ModelProvider] and [ToolManager]; it never
 * touches a provider SDK, a Minecraft class or a network implementation.
 *
 * Cancellation and failures that happen after an assistant message has been appended are repaired
 * with cancellation observations for the calls that have no reply yet, so the history stays
 * protocol-valid. Recorded calls are not replayed; the model may propose new calls in later steps.
 */
class ReactAgent(
    private val provider: ModelProvider,
    private val tools: ToolManager,
    private val settings: AgentSettings,
) {
    private val messages = ArrayList<ChatMessage>()
    private val turn = Mutex()
    private var adopted: LLMContext? = null

    /** Ordered conversation owned by this agent. Read-only view; never mutated through it. */
    val history: List<ChatMessage> get() = messages

    init {
        require(settings.maxSteps >= 1) { "maxSteps must be at least 1" }
        require(settings.requestTimeoutSeconds > 0) { "requestTimeoutSeconds must be positive" }
        require(settings.toolTimeoutSeconds > 0) { "toolTimeoutSeconds must be positive" }
    }

    /** Drops the conversation. Must not be called while [respond] is still running. */
    fun reset() {
        messages.clear()
        adopted = null
    }

    /**
     * The complete provider-independent context of this conversation: the prompt and tool set this agent
     * currently speaks with plus every recorded [ChatMessage]. Safe to serialize, hash and store; it never
     * contains credentials, and the returned list is a snapshot that the agent no longer shares.
     */
    fun exportContext(): LLMContext = buildContext()

    /**
     * Replaces the conversation with [context] and adopts its prompt and tool set, so a context that was
     * exported and stored elsewhere continues exactly where it stopped. Must not be called while [respond]
     * is still running.
     *
     * Adopted tool definitions are advertised to the model unchanged, but execution still goes through this
     * agent's own [ToolManager]; a call to a tool this client no longer registers becomes an unknown-tool
     * observation instead of being executed.
     */
    fun importContext(context: LLMContext) {
        messages.clear()
        messages.addAll(context.messages)
        adopted = context
    }

    /**
     * Prompt, advertised tools and messages as one context. The live message list is copied so callers can
     * encode or hash it without racing later steps.
     */
    private fun buildContext(): LLMContext {
        val inherited = adopted
        return LLMContext(
            systemPrompt = inherited?.systemPrompt ?: settings.systemPrompt,
            globalPrompt = inherited?.globalPrompt ?: emptyList(),
            tools = if (inherited != null) inherited.tools else tools.definitions.takeIf { it.isNotEmpty() },
            messages = messages.toList(),
        )
    }

    /**
     * Runs one turn for [message] and returns the text of the final assistant message; narration
     * streamed during earlier steps is reported through [AgentUpdate.TextDelta] only.
     *
     * [onUpdate] is invoked on the caller's thread as stream events arrive; it must not call back
     * into this agent. Turns are serialized: a turn started while another one is still running waits
     * for it, which keeps cancellation that has not unwound yet from corrupting the history.
     */
    suspend fun respond(message: String, onUpdate: (AgentUpdate) -> Unit = {}): String {
        return turn.withLock {
            messages += ChatMessage.User(message)
            repeat(settings.maxSteps) {
                val step = streamStep(onUpdate)
                when {
                    step.reason == FinishReason.ToolCalls -> {
                        if (step.calls.isEmpty()) {
                            throw AgentTurnException("The model requested tool calls but streamed none")
                        }
                        messages += ChatMessage.Assistant(step.text.ifEmpty { null }, step.calls)
                        runTools(step.calls, onUpdate)
                    }
                    // Tool calls the agent cannot store together with the reported reason would leave
                    // an assistant message without replies, so the step is rejected while the history
                    // is still untouched.
                    step.calls.isNotEmpty() -> throw AgentTurnException(
                        "The model streamed tool calls but finished with '${reasonName(step.reason)}'",
                    )
                    step.reason == FinishReason.Stop || step.reason == FinishReason.Length -> {
                        messages += ChatMessage.Assistant(step.text.ifEmpty { null })
                        return@withLock step.text
                    }
                    else -> throw unusable(step.reason)
                }
            }
            throw AgentTurnException("The agent reached its ${settings.maxSteps}-step limit without a final answer")
        }
    }

    /** Collects one complete model step: streamed text, indexed tool calls and the finish reason. */
    private suspend fun streamStep(onUpdate: (AgentUpdate) -> Unit): StreamedStep {
        val context = buildContext()
        val text = StringBuilder()
        val fragments = TreeMap<Int, CallFragments>()
        var reason: FinishReason? = null
        val completed = withTimeoutOrNull(settings.requestTimeoutSeconds * 1000) {
            val stream = provider.generate_stream(context).getOrElse { throw it }
            stream.collect { item ->
                when (val event = item.getOrElse { throw it }) {
                    is AssistantEvent.TextDelta -> {
                        text.append(event.text)
                        onUpdate(AgentUpdate.TextDelta(event.text))
                    }
                    is AssistantEvent.ToolCallDelta -> {
                        val fragment = fragments.getOrPut(event.index) { CallFragments() }
                        event.id?.let { value -> fragment.id.append(value) }
                        event.name?.let { value -> fragment.name.append(value) }
                        event.arguments?.let { value -> fragment.arguments.append(value) }
                    }
                    is AssistantEvent.Finished -> reason = event.reason
                    is AssistantEvent.TokenUsage -> Unit
                }
            }
            true
        }
        if (completed == null) throw AgentTurnException("The model request timed out")
        val finish = reason ?: throw AgentTurnException("The model stream ended without a finish reason")
        return StreamedStep(text.toString(), planCalls(fragments), finish)
    }

    /**
     * Turns streamed fragments into calls, in index order. A call the agent cannot correlate or name
     * is unusable and fails the step; arguments are joined and parsed only now, and one that is not a
     * JSON object is reported back to the model instead of being executed.
     */
    private fun planCalls(fragments: Map<Int, CallFragments>): List<ToolCall> {
        if (fragments.isEmpty()) return emptyList()
        val planned = ArrayList<ToolCall>(fragments.size)
        for ((index, fragment) in fragments) {
            if (fragment.id.isEmpty()) throw AgentTurnException("Streamed tool call $index has no call id")
            if (fragment.name.isEmpty()) throw AgentTurnException("Streamed tool call $index has no tool name")
            val id = fragment.id.toString()
            val name = fragment.name.toString()
            val raw = fragment.arguments.toString()
            val arguments = if (raw.isEmpty()) JsonObject(emptyMap()) else parseArguments(raw)
            planned += ToolCall(id, name, arguments)
        }
        return planned
    }

    private fun parseArguments(raw: String): JsonElement = try {
        Json.parseToJsonElement(raw)
    } catch (_: SerializationException) {
        throw AgentTurnException("Tool arguments must contain valid JSON")
    } catch (_: IllegalArgumentException) {
        throw AgentTurnException("Tool arguments must contain valid JSON")
    }

    /** Runs the calls of one assistant message sequentially, one reply each, in streamed order. */
    private suspend fun runTools(planned: List<ToolCall>, onUpdate: (AgentUpdate) -> Unit) {
        var replied = 0
        var started = false
        try {
            for (call in planned) {
                onUpdate(AgentUpdate.ToolStarted(call.name))
                started = true
                val reply = withTimeoutOrNull(settings.toolTimeoutSeconds * 1000) { tools.execute(call) }
                    ?: ChatMessage.Tool(
                        call.id,
                        toolFailureObservation(
                            call.name, "timeout",
                            "The tool timed out; whether it took effect is unknown. Do not retry automatically.",
                        ),
                    )
                messages += reply
                replied++
                started = false
                onUpdate(AgentUpdate.ToolFinished(call.name, reply.content))
            }
        } catch (failure: Throwable) {
            recordInterrupted(planned, replied, started)
            throw failure
        }
    }

    /** Answers calls that never produced a result, from the interrupted one onwards. */
    private fun recordInterrupted(planned: List<ToolCall>, from: Int, started: Boolean) {
        for (index in from until planned.size) {
            val call = planned[index]
            val message = if (index == from && started) {
                "The tool call was interrupted before it produced a result, so whether it took effect is unknown"
            } else {
                "The tool call was not executed because the turn was interrupted"
            }
            messages += ChatMessage.Tool(call.id, toolFailureObservation(call.name, "interrupted", message))
        }
    }

    private fun unusable(reason: FinishReason): AgentTurnException = when (reason) {
        FinishReason.ContentFilter -> AgentTurnException("The model response was blocked by the provider's content filter")
        FinishReason.Error -> AgentTurnException("The model provider reported an error finish reason")
        is FinishReason.Unknown -> AgentTurnException("The model finished with an unsupported reason")
        else -> AgentTurnException("The model finished with unusable reason '${reasonName(reason)}'")
    }

    private fun reasonName(reason: FinishReason): String = when (reason) {
        FinishReason.Stop -> "stop"
        FinishReason.ToolCalls -> "tool_calls"
        FinishReason.Length -> "length"
        FinishReason.ContentFilter -> "content_filter"
        FinishReason.Error -> "error"
        is FinishReason.Unknown -> "unknown"
    }
}

/** Raw id, name and argument fragments of one streamed tool call, joined in arrival order. */
private class CallFragments {
    val id = StringBuilder()
    val name = StringBuilder()
    val arguments = StringBuilder()
}

private class StreamedStep(val text: String, val calls: List<ToolCall>, val reason: FinishReason)
