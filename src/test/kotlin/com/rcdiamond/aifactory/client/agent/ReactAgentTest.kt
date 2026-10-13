package com.rcdiamond.aifactory.client.agent

import com.rcdiamond.aifactory.client.llm.AgentSettings
import com.rcdiamond.aifactory.client.llm.AgentUpdate
import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.GiveItemGateway
import com.rcdiamond.aifactory.client.llm.GiveItemResult
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.LLMTransportError
import com.rcdiamond.aifactory.client.llm.GenerateResponse
import com.rcdiamond.aifactory.client.llm.ModelProvider
import com.rcdiamond.aifactory.client.tool.GiveItemTool
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

private class TestAPIError(message: String) : LLMAPIError(message)

/** Replays scripted streamed steps and remembers every request the agent built. */
private class ScriptedProvider(private val steps: MutableList<Result<Flow<Result<AssistantEvent>>>>) : ModelProvider {
    val contexts = mutableListOf<LLMContext>()

    override suspend fun generate(prompt: LLMContext): GenerateResponse =
        throw UnsupportedOperationException("the agent only streams")

    override suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>> {
        contexts += prompt
        check(steps.isNotEmpty()) { "the agent asked for more steps than the test scripted" }
        return steps.removeAt(0)
    }
}

/** Records what the client asked the server for; the server side is out of scope here. */
private class RecordingGateway(
    private val result: suspend (String, Int) -> GiveItemResult = { _, _ -> GiveItemResult(UUID.randomUUID(), true, "ok") },
) : GiveItemGateway {
    val calls = mutableListOf<Pair<String, Int>>()

    override suspend fun giveItem(itemId: String, count: Int): GiveItemResult {
        calls += itemId to count
        return result(itemId, count)
    }
}

private fun testSettings(maxSteps: Int = 4, requestTimeoutSeconds: Long = 5, toolTimeoutSeconds: Long = 5) =
    AgentSettings("You are a test agent", maxSteps, requestTimeoutSeconds, toolTimeoutSeconds)

private fun answer(vararg events: AssistantEvent): Result<Flow<Result<AssistantEvent>>> =
    Result.success(flow { events.forEach { emit(Result.success(it)) } })

private fun text(value: String): AssistantEvent = AssistantEvent.TextDelta(value)

private fun toolCall(index: Int, id: String? = null, name: String? = null, arguments: String? = null): AssistantEvent =
    AssistantEvent.ToolCallDelta(index, id, name, arguments)

private fun finished(reason: FinishReason): AssistantEvent = AssistantEvent.Finished(reason)

private fun agentWith(
    provider: ModelProvider,
    gateway: GiveItemGateway,
    settings: AgentSettings = testSettings(),
): ReactAgent {
    val tools = ToolManager()
    tools.register(GiveItemTool(gateway))
    return ReactAgent(provider, tools, settings)
}

private fun observationOf(content: String): JsonObject = assertIs<JsonObject>(Json.parseToJsonElement(content))

private fun errorOf(content: String): String = assertIs<JsonPrimitive>(observationOf(content)["error"]).content

/** Every tool call of every assistant message must be followed by exactly one reply with its id. */
private fun assertHistoryValid(history: List<ChatMessage>) {
    var index = 0
    while (index < history.size) {
        when (val message = history[index]) {
            is ChatMessage.User -> index++
            is ChatMessage.Assistant -> {
                index++
                for (call in message.toolCalls) {
                    val reply = assertIs<ChatMessage.Tool>(history.getOrNull(index), "tool call ${call.id} has no reply")
                    assertEquals(call.id, reply.toolCallId)
                    index++
                }
            }
            is ChatMessage.Tool -> fail("tool reply ${message.toolCallId} has no preceding tool call")
        }
    }
}

class ReactAgentTest {
    @Test
    fun `final text is streamed in fragments on the caller thread and returned joined`() {
        val provider = ScriptedProvider(mutableListOf(answer(text("Hel"), text("lo"), finished(FinishReason.Stop))))
        val agent = agentWith(provider, RecordingGateway())
        val updates = mutableListOf<AgentUpdate>()
        val caller = Thread.currentThread()
        val threads = mutableSetOf<Thread>()
        lateinit var response: String

        runBlocking { response = agent.respond("hi") { update -> threads += Thread.currentThread(); updates += update } }

        assertEquals("Hello", response)
        assertEquals(listOf<AgentUpdate>(AgentUpdate.TextDelta("Hel"), AgentUpdate.TextDelta("lo")), updates)
        assertEquals(setOf(caller), threads)
        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi"), ChatMessage.Assistant("Hello")), agent.history)
    }

    @Test
    fun `a length finish reason ends the turn with the streamed text`() {
        val provider = ScriptedProvider(mutableListOf(answer(text("cut off"), finished(FinishReason.Length))))
        val agent = agentWith(provider, RecordingGateway())
        lateinit var response: String

        runBlocking { response = agent.respond("tell me a story") }

        assertEquals("cut off", response)
        assertEquals(listOf<ChatMessage>(ChatMessage.User("tell me a story"), ChatMessage.Assistant("cut off")), agent.history)
    }

    @Test
    fun `tool calls run in order, get one reply each and are fed into the next request`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(
                    toolCall(0, "call_a", "give_item", """{"item_id":"minecraft:diamond","count":3}"""),
                    toolCall(1, "call_b", "give_item", """{"item_id":"minecraft:stone","count":1}"""),
                    finished(FinishReason.ToolCalls),
                ),
                answer(text("Both "), text("given."), finished(FinishReason.Stop)),
            ),
        )
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway)
        val updates = mutableListOf<AgentUpdate>()
        lateinit var response: String

        runBlocking { response = agent.respond("two things please") { updates += it } }

        assertEquals("Both given.", response)
        assertEquals(listOf("minecraft:diamond" to 3, "minecraft:stone" to 1), gateway.calls)
        assertEquals(
            listOf("ToolStarted", "ToolFinished", "ToolStarted", "ToolFinished"),
            updates.filter { it is AgentUpdate.ToolStarted || it is AgentUpdate.ToolFinished }
                .map { if (it is AgentUpdate.ToolStarted) "ToolStarted" else "ToolFinished" },
        )

        val assistant = assertIs<ChatMessage.Assistant>(agent.history[1])
        assertEquals(listOf("call_a", "call_b"), assistant.toolCalls.map { it.id })
        assertEquals(listOf("give_item", "give_item"), assistant.toolCalls.map { it.name })
        assertEquals(
            buildJsonObject { put("item_id", "minecraft:diamond"); put("count", 3) },
            assistant.toolCalls[0].arguments,
        )
        val first = assertIs<ChatMessage.Tool>(agent.history[2])
        assertEquals("call_a", first.toolCallId)
        assertEquals(true, assertIs<JsonPrimitive>(observationOf(first.content)["success"]).boolean)
        val second = assertIs<ChatMessage.Tool>(agent.history[3])
        assertEquals("call_b", second.toolCallId)
        assertEquals("minecraft:stone", assertIs<JsonPrimitive>(observationOf(second.content)["item_id"]).content)

        // The second request carries the whole conversation up to that point, in order.
        assertEquals(4, provider.contexts[1].messages.size)
        assertEquals(agent.history.take(4), provider.contexts[1].messages)
        assertHistoryValid(agent.history)
    }

    @Test
    fun `an unknown tool is reported to the model and the turn can still finish`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(toolCall(0, "call_1", "teleport", """{"player":"me"}"""), finished(FinishReason.ToolCalls)),
                answer(text("I cannot do that."), finished(FinishReason.Stop)),
            ),
        )
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway)
        lateinit var response: String

        runBlocking { response = agent.respond("teleport me") }

        assertEquals("I cannot do that.", response)
        assertEquals("unknown_tool", errorOf(assertIs<ChatMessage.Tool>(agent.history[2]).content))
        assertTrue(gateway.calls.isEmpty())
        assertHistoryValid(agent.history)
    }

    @Test
    fun `malformed JSON arguments fail before any tool executes or assistant history is committed`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(
                    toolCall(0, "call_1", "give_item", "{\"item_id\":\"minecraft:diamond\",\"count\":"),
                    finished(FinishReason.ToolCalls),
                ),
            ),
        )
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway)

        assertFailsWith<AgentTurnException> { runBlocking { agent.respond("diamonds please") } }
        assertTrue(gateway.calls.isEmpty())
        assertEquals(listOf<ChatMessage>(ChatMessage.User("diamonds please")), agent.history)
        assertHistoryValid(agent.history)
    }

    @Test
    fun `fragments of one call are joined by index before the arguments are parsed`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(
                    toolCall(0, "ca", "give_item", "{\"item_id\":\"minecraft:diamond\","),
                    toolCall(0, id = "ll_1", arguments = "\"count\":2}"),
                    finished(FinishReason.ToolCalls),
                ),
                answer(text("Done."), finished(FinishReason.Stop)),
            ),
        )
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway)

        runBlocking { agent.respond("diamonds") }

        assertEquals(listOf("minecraft:diamond" to 2), gateway.calls)
        val assistant = assertIs<ChatMessage.Assistant>(agent.history[1])
        assertEquals(listOf("call_1"), assistant.toolCalls.map { it.id })
        assertHistoryValid(agent.history)
    }

    @Test
    fun `a stream without a finish reason fails the turn and leaves the history valid`() {
        val provider = ScriptedProvider(mutableListOf(answer(text("partial"))))
        val agent = agentWith(provider, RecordingGateway())

        assertFailsWith<AgentTurnException> { runBlocking { agent.respond("hi") } }

        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history)
        assertHistoryValid(agent.history)
    }

    @ParameterizedTest
    @MethodSource("unusableReasons")
    fun `finish reasons the agent cannot act on fail the turn`(reason: FinishReason) {
        val provider = ScriptedProvider(mutableListOf(answer(text("something"), finished(reason))))
        val agent = agentWith(provider, RecordingGateway())

        assertFailsWith<AgentTurnException> { runBlocking { agent.respond("hi") } }

        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history)
    }

    @Test
    fun `steps that cannot become protocol-valid history are rejected before anything is appended`() {
        val scenarios = listOf(
            "tool calls with a stop reason" to answer(
                toolCall(0, "call_1", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
                finished(FinishReason.Stop),
            ),
            "tool_calls without any call" to answer(finished(FinishReason.ToolCalls)),
            "call without an id" to answer(toolCall(0, null, "give_item", "{}"), finished(FinishReason.ToolCalls)),
            "call without a name" to answer(toolCall(0, "call_1", null, "{}"), finished(FinishReason.ToolCalls)),
        )

        for ((label, step) in scenarios) {
            val gateway = RecordingGateway()
            val agent = agentWith(ScriptedProvider(mutableListOf(step)), gateway)

            assertFailsWith<AgentTurnException>(label) { runBlocking { agent.respond("hi") } }

            assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history, label)
            assertTrue(gateway.calls.isEmpty(), label)
        }
    }

    @Test
    fun `a failed stream item propagates and keeps partial text out of the history`() {
        val provider = ScriptedProvider(
            mutableListOf(
                Result.success<Flow<Result<AssistantEvent>>>(
                    flow {
                        emit(Result.success<AssistantEvent>(AssistantEvent.TextDelta("half")))
                        emit(Result.failure<AssistantEvent>(TestAPIError("upstream refused")))
                    },
                ),
            ),
        )
        val agent = agentWith(provider, RecordingGateway())

        assertFailsWith<TestAPIError> { runBlocking { agent.respond("hi") } }

        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history)
        assertHistoryValid(agent.history)
    }

    @Test
    fun `a setup failure propagates before any message is appended`() {
        val provider = ScriptedProvider(mutableListOf<Result<Flow<Result<AssistantEvent>>>>(Result.failure(LLMTransportError())))
        val agent = agentWith(provider, RecordingGateway())

        assertFailsWith<LLMTransportError> { runBlocking { agent.respond("hi") } }

        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history)
    }

    @Test
    fun `the step limit ends the turn after every reply is recorded`() {
        val step = answer(
            toolCall(0, "call_1", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
            finished(FinishReason.ToolCalls),
        )
        val provider = ScriptedProvider(mutableListOf(step, step, step))
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway, testSettings(maxSteps = 2))

        assertFailsWith<AgentTurnException> { runBlocking { agent.respond("diamonds") } }

        assertEquals(2, gateway.calls.size)
        assertEquals(5, agent.history.size)
        assertHistoryValid(agent.history)
    }

    @Test
    fun `a tool that outlives its timeout is reported and the turn continues`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(
                    toolCall(0, "call_1", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
                    finished(FinishReason.ToolCalls),
                ),
                answer(text("I stopped waiting."), finished(FinishReason.Stop)),
            ),
        )
        val entered = CompletableDeferred<Unit>()
        val gateway = RecordingGateway { _, _ ->
            entered.complete(Unit)
            awaitCancellation()
        }
        val agent = agentWith(provider, gateway, testSettings(toolTimeoutSeconds = 1))
        lateinit var response: String

        runBlocking { response = agent.respond("diamonds") }

        assertEquals("I stopped waiting.", response)
        assertTrue(entered.isCompleted)
        assertEquals("timeout", errorOf(assertIs<ChatMessage.Tool>(agent.history[2]).content))
        assertHistoryValid(agent.history)
    }

    @Test
    fun `a model that never answers hits the request timeout and leaves the history valid`() {
        val provider = ScriptedProvider(
            mutableListOf(Result.success<Flow<Result<AssistantEvent>>>(flow { awaitCancellation() })),
        )
        val agent = agentWith(provider, RecordingGateway(), testSettings(requestTimeoutSeconds = 1))

        assertFailsWith<AgentTurnException> { runBlocking { agent.respond("hi") } }

        assertEquals(listOf<ChatMessage>(ChatMessage.User("hi")), agent.history)
    }

    @Test
    fun `cancelling a turn answers the unsettled calls without running them again`() {
        val provider = ScriptedProvider(
            mutableListOf(
                answer(
                    toolCall(0, "call_a", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
                    toolCall(1, "call_b", "give_item", """{"item_id":"minecraft:stone","count":1}"""),
                    toolCall(2, "call_c", "give_item", """{"item_id":"minecraft:dirt","count":1}"""),
                    finished(FinishReason.ToolCalls),
                ),
                answer(text("Sure."), finished(FinishReason.Stop)),
            ),
        )
        var calls = 0
        val entered = CompletableDeferred<Unit>()
        val gateway = RecordingGateway { _, _ ->
            calls += 1
            if (calls == 2) {
                entered.complete(Unit)
                awaitCancellation()
            }
            GiveItemResult(UUID.randomUUID(), true, "ok")
        }
        val agent = agentWith(provider, gateway)
        val updates = mutableListOf<AgentUpdate>()

        runBlocking {
            val cancelled = CompletableDeferred<Throwable>()
            val job = launch {
                try {
                    agent.respond("three things") { updates += it }
                } catch (failure: Throwable) {
                    cancelled.complete(failure)
                    throw failure
                }
            }
            entered.await()
            job.cancelAndJoin()
            assertIs<CancellationException>(cancelled.await())
        }

        assertEquals(2, calls)
        assertEquals(5, agent.history.size)
        val completed = assertIs<ChatMessage.Tool>(agent.history[2])
        assertEquals("call_a", completed.toolCallId)
        assertEquals(true, assertIs<JsonPrimitive>(observationOf(completed.content)["success"]).boolean)
        val interrupted = assertIs<ChatMessage.Tool>(agent.history[3])
        assertEquals("call_b", interrupted.toolCallId)
        assertEquals("interrupted", errorOf(interrupted.content))
        val neverRan = assertIs<ChatMessage.Tool>(agent.history[4])
        assertEquals("call_c", neverRan.toolCallId)
        assertHistoryValid(agent.history)
        assertEquals(2, updates.filterIsInstance<AgentUpdate.ToolStarted>().size)

        lateinit var response: String
        runBlocking { response = agent.respond("again") }
        assertEquals("Sure.", response)
        assertEquals(7, agent.history.size)
        assertEquals(agent.history.take(6), provider.contexts[1].messages)
        assertHistoryValid(agent.history)
    }

    @Test
    fun `an enclosing timeout cancels tools without becoming a tool timeout observation`() = runBlocking {
        val provider = ScriptedProvider(mutableListOf(answer(
            toolCall(0, "call_1", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
            finished(FinishReason.ToolCalls),
        )))
        val gateway = RecordingGateway { _, _ -> awaitCancellation() }
        val agent = agentWith(provider, gateway)
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(500) { agent.respond("diamonds") }
        }
        assertEquals(1, gateway.calls.size)
        assertEquals(1, provider.contexts.size)
        assertEquals("interrupted", errorOf(assertIs<ChatMessage.Tool>(agent.history[2]).content))
        assertHistoryValid(agent.history)
    }

    @Test
    fun `a failing observer cannot erase a completed tool result or leave unanswered calls`() = runBlocking {
        val provider = ScriptedProvider(mutableListOf(answer(
            toolCall(0, "call_a", "give_item", """{"item_id":"minecraft:diamond","count":1}"""),
            toolCall(1, "call_b", "give_item", """{"item_id":"minecraft:stone","count":1}"""),
            finished(FinishReason.ToolCalls),
        )))
        val gateway = RecordingGateway()
        val agent = agentWith(provider, gateway)
        assertFailsWith<IllegalStateException> {
            agent.respond("two items") { if (it is AgentUpdate.ToolFinished) error("observer failed") }
        }
        assertEquals(1, gateway.calls.size)
        assertEquals(true, assertIs<JsonPrimitive>(observationOf(assertIs<ChatMessage.Tool>(agent.history[2]).content)["success"]).boolean)
        assertEquals("interrupted", errorOf(assertIs<ChatMessage.Tool>(agent.history[3]).content))
        assertHistoryValid(agent.history)
    }

    @Test
    fun `reset drops the conversation`() {
        val provider = ScriptedProvider(
            mutableListOf(answer(text("one"), finished(FinishReason.Stop)), answer(text("two"), finished(FinishReason.Stop))),
        )
        val agent = agentWith(provider, RecordingGateway())

        runBlocking { agent.respond("first") }
        assertEquals(2, agent.history.size)

        agent.reset()
        assertEquals(emptyList<ChatMessage>(), agent.history)

        runBlocking { agent.respond("second") }
        assertEquals(listOf<ChatMessage>(ChatMessage.User("second"), ChatMessage.Assistant("two")), agent.history)
        assertEquals(listOf<ChatMessage>(ChatMessage.User("second")), provider.contexts[1].messages)
    }

    companion object {
        @JvmStatic
        fun unusableReasons(): Stream<FinishReason> = Stream.of(
            FinishReason.ContentFilter,
            FinishReason.Error,
            FinishReason.Unknown("function_call"),
        )
    }
}
