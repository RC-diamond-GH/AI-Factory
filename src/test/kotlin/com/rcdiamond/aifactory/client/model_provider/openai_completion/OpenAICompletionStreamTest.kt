package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.Usage
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Consumer-visible events of one stream, with streamed tool calls rebuilt from their raw argument fragments. */
internal class CollectedStream(val events: List<AssistantEvent>, val failures: List<Throwable>) {
    val textDeltas: List<AssistantEvent.TextDelta> = events.filterIsInstance<AssistantEvent.TextDelta>()
    val toolCallDeltas: List<AssistantEvent.ToolCallDelta> = events.filterIsInstance<AssistantEvent.ToolCallDelta>()
    val finishedReasons: List<FinishReason> = events.filterIsInstance<AssistantEvent.Finished>().map { it.reason }
    val usage: Usage? = events.filterIsInstance<AssistantEvent.TokenUsage>().firstOrNull()?.usage
    val text: String = textDeltas.joinToString("") { it.text }

    fun requireNoFailures(label: String = "stream"): CollectedStream {
        assertTrue(failures.isEmpty(), "$label failed: ${failures.map { it::class.simpleName }}")
        return this
    }

    /** One tool call per streamed index; the raw argument fragments are joined and parsed as JSON. */
    fun toolCallsByIndex(): Map<Int, ToolCall> = toolCallDeltas.groupBy { it.index }.toSortedMap().mapValues { (index, deltas) ->
        val id = deltas.mapNotNull { it.id }.joinToString("").takeIf { it.isNotEmpty() }
            ?: error("streamed tool call $index has no id")
        val name = deltas.mapNotNull { it.name }.joinToString("").takeIf { it.isNotEmpty() }
            ?: error("streamed tool call $index has no name")
        val arguments = deltas.mapNotNull { it.arguments }.joinToString("")
        ToolCall(id, name, Json.parseToJsonElement(arguments))
    }
}

internal suspend fun collectAssistantEvents(flow: Flow<Result<AssistantEvent>>): CollectedStream {
    val events = mutableListOf<AssistantEvent>()
    val failures = mutableListOf<Throwable>()
    flow.collect { item -> item.fold({ events += it }, { failures += it }) }
    return CollectedStream(events, failures)
}

/**
 * Streaming transport tests against a local HTTP server: incremental delivery, SSE framing,
 * terminal failures and cancellation are observed through [OpenAICompletion.generate_stream].
 */
class OpenAICompletionStreamTest {
    private val prompt = LLMContext("system", messages = listOf(ChatMessage.User("hello")))

    @Test
    fun `events are delivered before the server completes the response`() = runBlocking {
        withTimeout(30.seconds) {
            val firstEventObserved = CountDownLatch(1)
            val serverFinished = AtomicBoolean(false)
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"he"},"finish_reason":null}]}"""))
                // The rest of the response is withheld until the client consumed the first event.
                check(firstEventObserved.await(15, TimeUnit.SECONDS)) { "client never observed the first event" }
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"llo"},"finish_reason":null}]}"""))
                out.fragment(sse("""{"choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}"""))
                out.fragment("data: [DONE]\n\n")
                serverFinished.set(true)
            }) { provider ->
                val events = mutableListOf<AssistantEvent>()
                provider.generate_stream(prompt).getOrThrow().collect { item ->
                    val event = item.getOrThrow()
                    if (events.isEmpty()) {
                        assertFalse(serverFinished.get(), "the first event only arrived after the whole response")
                        firstEventObserved.countDown()
                    }
                    events += event
                }
                val collected = CollectedStream(events, emptyList())
                assertEquals("hello", collected.text)
                assertEquals(listOf<FinishReason>(FinishReason.Stop), collected.finishedReasons)
            }
        }
    }

    @Test
    fun `SSE framing survives byte splits, comments, CRLF and multiline data`() = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                val wire = buildString {
                    append(": keep-alive comment\r\n")
                    append("\r\n")
                    append(sse("""{"choices":[{"index":0,"delta":{"content":"héllo 😀"},"finish_reason":null}]}"""))
                    append(":\n")
                    append("data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"multi\"},\n")
                    append("data: \"finish_reason\":null}]}\r\n\r\n")
                    append(sse("""{"choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}"""))
                    append(sse("""{"choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}"""))
                    append("data: [DONE]\n\n")
                }
                val bytes = wire.toByteArray(StandardCharsets.UTF_8)
                val splits = sortedSetOf(
                    byteIndexOf(wire, "é") + 1,
                    byteIndexOf(wire, "😀") + 1,
                    byteIndexOf(wire, "😀") + 2,
                    byteIndexOf(wire, "\r\n") + 1,
                    byteIndexOf(wire, "[DONE]") + 1,
                    bytes.size - 1,
                )
                var written = 0
                for (split in splits) {
                    if (split <= written || split >= bytes.size) continue
                    out.write(bytes, written, split - written)
                    out.flush()
                    written = split
                }
                out.write(bytes, written, bytes.size - written)
                out.flush()
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow()).requireNoFailures()
                assertEquals(listOf("héllo 😀", "multi", "!"), collected.textDeltas.map { it.text })
                assertEquals(listOf<FinishReason>(FinishReason.Stop), collected.finishedReasons)
            }
        }
    }

    @Test
    fun `usage-only chunks and unknown finish reasons are preserved`() = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"42"},"finish_reason":"future_reason"}]}"""))
                out.fragment(sse("""{"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":3,"total_tokens":15}}"""))
                out.fragment("data: [DONE]\n\n")
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow()).requireNoFailures()
                assertEquals("42", collected.text)
                assertEquals(listOf<FinishReason>(FinishReason.Unknown("future_reason")), collected.finishedReasons)
                assertEquals(Usage(12uL, 3uL, 15uL), collected.usage)
            }
        }
    }

    @Test
    fun `HTTP errors terminate the stream with a sanitized failure`() = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val body = "upstream-secret-response-body".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(401, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow())
                assertEquals(1, collected.failures.size, "expected exactly one terminal failure")
                assertTrue(collected.events.isEmpty(), "no event may be delivered for an HTTP error")
                val failure = assertIs<OpenAIHttpException>(collected.failures.single())
                assertEquals(401, failure.statusCode)
                assertSanitized(failure, "upstream-secret-response-body", "test-key")
            }
        }
    }

    @Test
    fun `a stream truncated before DONE fails the stream`() = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"leak-canary"},"finish_reason":null}]}"""))
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow())
                assertEquals("leak-canary", collected.text)
                assertEquals(1, collected.failures.size, "expected exactly one terminal failure")
                val failure = assertIs<LLMAPIError>(collected.failures.single())
                assertSanitized(failure, "leak-canary", "test-key")
            }
        }
    }

    @Test
    fun `DONE without a finish reason is a terminal failure`() = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"x"},"finish_reason":null}]}"""))
                out.fragment("data: [DONE]\n\n")
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow())
                assertEquals("x", collected.text)
                assertEquals(1, collected.failures.size, "expected exactly one terminal failure")
                assertSanitized(assertIs<LLMAPIError>(collected.failures.single()))
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "not-json",
        "{\"choices\":\"nope\"}",
        "{\"error\":{\"message\":\"provider-secret-error-body\"}}",
    ])
    fun `malformed stream payloads terminate with a sanitized failure`(payload: String) = runBlocking {
        withTimeout(30.seconds) {
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse(payload))
                out.fragment("data: [DONE]\n\n")
            }) { provider ->
                val collected = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow())
                assertEquals(1, collected.failures.size, "expected exactly one terminal failure")
                assertSanitized(assertIs<LLMAPIError>(collected.failures.single()), payload, "test-key")
            }
        }
    }

    @Test
    fun `each collection starts its own request`() = runBlocking {
        withTimeout(30.seconds) {
            val requests = AtomicInteger()
            val requested = AtomicBoolean(false)
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                requests.incrementAndGet()
                requested.set(true)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"hi"},"finish_reason":"stop"}]}"""))
                out.fragment("data: [DONE]\n\n")
            }) { provider ->
                val flow = provider.generate_stream(prompt).getOrThrow()
                assertFalse(requested.get(), "the request must not start before the flow is collected")
                assertEquals("hi", collectAssistantEvents(flow).requireNoFailures("first collection").text)
                assertEquals("hi", collectAssistantEvents(flow).requireNoFailures("second collection").text)
                assertEquals(2, requests.get(), "each collection must issue its own request")
            }
        }
    }

    @Test
    fun `cancelling collection releases the stalled subscription`() = runBlocking {
        withTimeout(60.seconds) {
            val released = CountDownLatch(1)
            withStreamingServer(handler = { exchange ->
                drainRequest(exchange)
                val out = exchange.beginEventStream()
                out.fragment(sse("""{"choices":[{"index":0,"delta":{"content":"start"},"finish_reason":null}]}"""))
                val padding = ": " + "x".repeat(64 * 1024) + "\n"
                val deadline = System.nanoTime() + 45_000_000_000L
                try {
                    while (System.nanoTime() < deadline) {
                        // Fails once the cancelled client drops its HTTP subscription.
                        out.fragment(padding)
                    }
                } catch (expected: IOException) {
                    released.countDown()
                }
            }) { provider ->
                coroutineScope {
                    val collected = mutableListOf<Result<AssistantEvent>>()
                    val firstEvent = CountDownLatch(1)
                    val flow = provider.generate_stream(prompt).getOrThrow()
                    val job = launch(Dispatchers.IO) {
                        flow.collect { item ->
                            collected += item
                            firstEvent.countDown()
                        }
                    }
                    assertTrue(firstEvent.await(15, TimeUnit.SECONDS), "the client never received the first event")
                    withTimeout(15.seconds) { job.cancelAndJoin() }
                    assertTrue(job.isCancelled, "cancelling the collector must cancel the flow")
                    assertTrue(released.await(15, TimeUnit.SECONDS), "the server kept writing after cancellation")
                    assertTrue(collected.none { it.isFailure }, "cancellation produced a failure item")
                }
            }
        }
    }

    private fun assertSanitized(failure: Throwable, vararg secrets: String) {
        assertIs<LLMAPIError>(failure)
        val text = generateSequence<Throwable>(failure) { it.cause }
            .joinToString("\n") { "${it::class.qualifiedName}: ${it.message}" }
        for (secret in secrets) {
            assertFalse(text.contains(secret), "the failure leaked upstream content: $text")
        }
    }

    private suspend fun withStreamingServer(
        handler: (HttpExchange) -> Unit,
        block: suspend (OpenAICompletion) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newCachedThreadPool()
        val handlerFailure = AtomicReference<Throwable>()
        server.executor = executor
        server.createContext("/v1/chat/completions") { exchange ->
            try {
                handler(exchange)
            } catch (failure: Throwable) {
                handlerFailure.set(failure)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            block(OpenAICompletion("http://127.0.0.1:${server.address.port}/v1/", "test-key", "test-model"))
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
        handlerFailure.get()?.let { throw AssertionError("the streaming test server failed", it) }
    }

    private fun drainRequest(exchange: HttpExchange) {
        exchange.requestBody.use { it.transferTo(OutputStream.nullOutputStream()) }
    }

    private fun HttpExchange.beginEventStream(): OutputStream {
        responseHeaders.add("Content-Type", "text/event-stream; charset=utf-8")
        responseHeaders.add("Cache-Control", "no-cache")
        sendResponseHeaders(200, 0)
        return responseBody
    }

    private fun OutputStream.fragment(text: String) {
        write(text.toByteArray(StandardCharsets.UTF_8))
        flush()
    }

    private fun sse(payload: String): String = "data: $payload\n\n"

    private fun byteIndexOf(wire: String, marker: String): Int =
        wire.substring(0, wire.indexOf(marker)).toByteArray(StandardCharsets.UTF_8).size
}
