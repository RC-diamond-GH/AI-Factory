package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class OpenAICompletionTest {
    @Test
    fun `tool-only assistant retains null content and parses arguments from a JSON string`() {
        val response = OpenAICompletionCodec.decode("""{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"add","arguments":"{\"a\":7,\"b\":11}"}}]},"finish_reason":"tool_calls"}]}""")
        val assistant = assertIs<ChatMessage.Assistant>(response.message)
        assertNull(assistant.content)
        assertEquals(FinishReason.ToolCalls, response.finishReason)
        assertNull(response.usage)
        val call = assistant.toolCalls.single()
        assertEquals("call_1", call.id)
        assertEquals("add", call.name)
        assertEquals(Json.parseToJsonElement("""{"a":7,"b":11}"""), call.arguments)
    }

    @Test
    fun `unknown finish reason is preserved without inventing stop`() {
        assertEquals(FinishReason.Unknown("future_reason"), OpenAICompletionCodec.decode(response("future_reason")).finishReason)
    }

    @ParameterizedTest
    @ValueSource(strings = ["stop", "length", "content_filter", "error"])
    fun `known termination reasons stay distinct`(reason: String) {
        val expected = when (reason) {
            "stop" -> FinishReason.Stop
            "length" -> FinishReason.Length
            "content_filter" -> FinishReason.ContentFilter
            else -> FinishReason.Error
        }
        assertEquals(expected, OpenAICompletionCodec.decode(response(reason)).finishReason)
    }

    @Test
    fun `unsigned usage preserves values above signed long maximum`() {
        val result = OpenAICompletionCodec.decode(response(usage = """{"prompt_tokens":9223372036854775808,"completion_tokens":0,"total_tokens":18446744073709551615}"""))
        assertEquals(9223372036854775808uL, result.usage?.inputTokens)
        assertEquals(0uL, result.usage?.outputTokens)
        assertEquals(ULong.MAX_VALUE, result.usage?.totalTokens)
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "1.5", "18446744073709551616", "\"12\"", "null"])
    fun `invalid token counts are rejected rather than truncated`(value: String) {
        assertFailsWith<OpenAIProtocolException> {
            OpenAICompletionCodec.decode(response(usage = """{"prompt_tokens":$value,"completion_tokens":1,"total_tokens":2}"""))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "{}",
        "{\"choices\":[]}",
        "{\"choices\":[{\"message\":{\"role\":\"user\",\"content\":\"wrong role\"},\"finish_reason\":\"stop\"}]}",
        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":17},\"finish_reason\":\"stop\"}]}",
        "not JSON",
    ])
    fun `malformed responses cannot become successful empty answers`(body: String) {
        assertFailsWith<OpenAIProtocolException> { OpenAICompletionCodec.decode(body) }
    }

    @Test
    fun `malformed tool arguments surface as a protocol error`() {
        assertFailsWith<OpenAIProtocolException> {
            OpenAICompletionCodec.decode("""{"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"c","type":"function","function":{"name":"f","arguments":"{"}}]},"finish_reason":"tool_calls"}]}""")
        }
    }

    @Test
    fun `HTTP errors expose status without leaking upstream response contents`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            exchange.requestBody.use { it.readAllBytes() }
            val body = "secret-key-and-private-prompt".toByteArray()
            exchange.sendResponseHeaders(401, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
            exchange.close()
        }
        server.start()
        try {
            val provider = OpenAICompletion("http://127.0.0.1:${server.address.port}/v1/", "test-key", "test-model")
            val failure = assertFailsWith<OpenAIHttpException> {
                provider.generate(LLMContext("system", messages = listOf(ChatMessage.User("hello"))))
            }
            assertEquals(401, failure.statusCode)
            assertFalse(failure.toString().contains("secret-key-and-private-prompt"))
            assertFalse(failure.toString().contains("test-key"))
        } finally {
            server.stop(0)
        }
    }

    private fun response(reason: String = "stop", usage: String = "null"): String =
        """{"choices":[{"message":{"role":"assistant","content":"answer"},"finish_reason":"$reason"}],"usage":$usage}"""
}
