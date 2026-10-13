package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.Usage
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class OpenAICompletionStreamDecoderTest {
    @Test
    fun `text and tool arguments arrive as fragments and arguments are never parsed`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.TextDelta("Hel")),
            decoder.decode("""{"choices":[{"index":0,"delta":{"role":"assistant","content":"Hel"}}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.TextDelta("lo")),
            decoder.decode("""{"choices":[{"index":0,"delta":{"content":"lo"}}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.ToolCallDelta(0, "call_1", "add", "{\"a\"")),
            decoder.decode("""{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"add","arguments":"{\"a\""}}]}}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.ToolCallDelta(0, arguments = ":7}")),
            decoder.decode("""{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":":7}"}}]}}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.Finished(FinishReason.ToolCalls)),
            decoder.decode(finishChunk("tool_calls")),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.TokenUsage(Usage(12uL, 3uL, 15uL))),
            decoder.decode("""{"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":3,"total_tokens":15}}"""),
        )
        decoder.finish()
    }

    @Test
    fun `interleaved tool call indexes keep their own fragments`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertEquals(
            listOf<AssistantEvent>(
                AssistantEvent.ToolCallDelta(0, "call_a", "add", "{\"a\":"),
                AssistantEvent.ToolCallDelta(1, "call_b", "add", "{\"b\":"),
            ),
            decoder.decode("""{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_a","type":"function","function":{"name":"add","arguments":"{\"a\":"}},{"index":1,"id":"call_b","type":"function","function":{"name":"add","arguments":"{\"b\":"}}]}}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(
                AssistantEvent.ToolCallDelta(1, arguments = "2}"),
                AssistantEvent.ToolCallDelta(0, arguments = "1}"),
            ),
            decoder.decode("""{"choices":[{"index":0,"delta":{"tool_calls":[{"index":1,"function":{"arguments":"2}"}},{"index":0,"function":{"arguments":"1}"}}]}}]}"""),
        )
    }

    @Test
    fun `role-only and empty deltas produce no visible events`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertEquals(
            emptyList<AssistantEvent>(),
            decoder.decode("""{"choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}"""),
        )
        assertEquals(emptyList<AssistantEvent>(), decoder.decode("""{"choices":[{"index":0,"delta":{"content":null}}]}"""))
        assertEquals(emptyList<AssistantEvent>(), decoder.decode("""{"choices":[]}"""))
    }

    @Test
    fun `unknown finish reasons survive the stream`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.Finished(FinishReason.Unknown("future_reason"))),
            decoder.decode(finishChunk("future_reason")),
        )
        decoder.finish()
    }

    @Test
    fun `finish stays unsatisfied until finish_reason arrives`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertFailsWith<OpenAIProtocolException> { decoder.finish() }
        decoder.decode("""{"choices":[{"index":0,"delta":{"content":"partial"}}]}""")
        assertFailsWith<OpenAIProtocolException> { decoder.finish() }
        decoder.decode(finishChunk("length"))
        decoder.finish()
    }

    @Test
    fun `choices other than index zero never produce events or finish the stream`() {
        val decoder = OpenAICompletionStreamDecoder()
        assertEquals(
            emptyList<AssistantEvent>(),
            decoder.decode("""{"choices":[{"index":1,"delta":{"content":"ignored"},"finish_reason":"stop"}]}"""),
        )
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.TextDelta("kept")),
            decoder.decode("""{"choices":[{"index":1,"delta":{"content":"ignored"}},{"index":0,"delta":{"content":"kept"}}]}"""),
        )
        assertFailsWith<OpenAIProtocolException> { decoder.finish() }
    }

    @Test
    fun `chunks after finish_reason are rejected except the usage-only chunk`() {
        val decoder = OpenAICompletionStreamDecoder()
        decoder.decode(finishChunk("stop"))
        assertEquals(emptyList<AssistantEvent>(), decoder.decode("""{"choices":[]}"""))
        assertEquals(
            listOf<AssistantEvent>(AssistantEvent.TokenUsage(Usage(1uL, 1uL, 2uL))),
            decoder.decode("""{"choices":[],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}"""),
        )
        assertFailsWith<OpenAIProtocolException> {
            decoder.decode("""{"choices":[{"index":0,"delta":{"content":"late"}}]}""")
        }
        assertFailsWith<OpenAIProtocolException> { decoder.decode(finishChunk("stop")) }
    }

    @ParameterizedTest
    @MethodSource("malformedChunks")
    fun `malformed chunks are rejected instead of yielding a partial answer`(chunk: String) {
        assertFailsWith<OpenAIProtocolException> { OpenAICompletionStreamDecoder().decode(chunk) }
    }

    @Test
    fun `in-band errors are rejected without echoing upstream text`() {
        val failure = assertFailsWith<OpenAIProtocolException> {
            OpenAICompletionStreamDecoder()
                .decode("""{"error":{"message":"invalid api key sk-secret","type":"invalid_request_error"}}""")
        }
        assertFalse(failure.toString().contains("sk-secret"))
        assertFalse(failure.toString().contains("invalid_request_error"))
    }

    private fun finishChunk(reason: String): String =
        """{"choices":[{"index":0,"delta":{},"finish_reason":"$reason"}]}"""

    companion object {
        @JvmStatic
        fun malformedChunks(): Stream<String> = Stream.of(
            "not JSON",
            """{"choices":{}}""",
            """{"choices":[{"index":0,"delta":17}]}""",
            """{"choices":[{"index":"0","delta":{}}]}""",
            """{"choices":[{"index":0,"delta":{"role":17}}]}""",
            """{"choices":[{"index":0,"delta":{"role":"user","content":"wrong role"}}]}""",
            """{"choices":[{"index":0,"delta":{"content":0}}]}""",
            """{"choices":[{"index":0,"delta":{"tool_calls":{"index":0}}}]}""",
            """{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"type":"retrieval","function":{"name":"add"}}]}}]}""",
            """{"choices":[{"index":0,"delta":{"tool_calls":[{"index":-1}]}}]}""",
            """{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":5}]}}]}""",
            """{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":{}}}]}}]}""",
            """{"choices":[{"index":0,"delta":{},"finish_reason":7}]}""",
            """{"choices":[{"index":0,"delta":{},"finish_reason":null}],"usage":{"prompt_tokens":-1,"completion_tokens":0,"total_tokens":0}}""",
            """{"error":{"message":"invalid api key"}}""",
        )
    }
}
