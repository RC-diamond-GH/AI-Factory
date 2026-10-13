package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.GlobalPrompt
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.llm.Usage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Builds [OpenAICompletion] from the .env file named by the `openai.envFile` system property,
 * which the `openaiIntegrationTest` Gradle task sets to the repository root .env. Values are
 * never logged, and the default `test` task never loads them.
 */
private object OpenAILiveProvider {
    private val values: Map<String, String> by lazy {
        val envFile = Path.of(requireNotNull(System.getProperty("openai.envFile")) { "openai.envFile system property is required" })
        Files.readAllLines(envFile).mapNotNull { line ->
            val text = line.trim().removePrefix("\uFEFF").removePrefix("export ")
            if (text.isEmpty() || text.startsWith('#')) return@mapNotNull null
            val separator = text.indexOf('=')
            if (separator < 1) return@mapNotNull null
            val key = text.substring(0, separator).trim()
            var value = text.substring(separator + 1).trim()
            if (value.length >= 2 && ((value.first() == '"' && value.last() == '"') ||
                        (value.first() == '\'' && value.last() == '\''))) {
                value = value.substring(1, value.length - 1)
            }
            key to value
        }.toMap()
    }

    fun create(): OpenAICompletion = OpenAICompletion(
        baseUrl = required("OPENAI_BASE_URL"),
        apiKey = required("OPENAI_API_KEY"),
        model = required("OPENAI_MODEL"),
    )

    private fun required(key: String): String = values[key]?.takeIf { it.isNotBlank() }
        ?: error("Missing $key in .env")
}

/** Shared prompt for both live tests: force one add tool call, then answer with the resulting integer. */
private fun calculatorPrompt(): LLMContext = LLMContext(
    systemPrompt = "You are a calculator. Always use the add tool for addition. " +
        "After receiving the tool result, respond only with the resulting integer, no markdown. " +
        "Do not call the tool again after receiving its result.",
    globalPrompt = listOf(GlobalPrompt("calculator.txt", "The operands are a=7 and b=11.")),
    tools = listOf(ToolDefinition("add", "Add two integers.", Json.parseToJsonElement(
        """{"type":"object","properties":{"a":{"type":"integer"},"b":{"type":"integer"}},"required":["a","b"],"additionalProperties":false}"""
    ))),
    messages = listOf(ChatMessage.User("Use add to sum the operands specified in calculator.txt.")),
)

@Tag("openai-live")
class OpenAICompletionLiveTest {
    @Test
    fun `real API completes a tool call and consumes its result`() = runBlocking {
        val provider = OpenAILiveProvider.create()
        val prompt = calculatorPrompt()
        val first = provider.generate(prompt)
        assertEquals(FinishReason.ToolCalls, first.finishReason)
        val assistant = assertIs<ChatMessage.Assistant>(first.message)
        assertEquals(1, assistant.toolCalls.size)
        val call = assistant.toolCalls.single()
        assertTrue(call.id.isNotBlank())
        assertEquals("add", call.name)
        val arguments = call.arguments.jsonObject
        assertEquals(7, arguments.getValue("a").jsonPrimitive.int)
        assertEquals(11, arguments.getValue("b").jsonPrimitive.int)
        val sum = arguments.getValue("a").jsonPrimitive.int + arguments.getValue("b").jsonPrimitive.int
        val second = provider.generate(prompt.copy(
            messages = prompt.messages + assistant + ChatMessage.Tool(call.id, sum.toString()),
        ))
        assertEquals(FinishReason.Stop, second.finishReason)
        val answer = assertIs<ChatMessage.Assistant>(second.message)
        assertEquals(18, answer.content?.trim()?.toIntOrNull())
        assertTrue(answer.toolCalls.isEmpty())
        for (response in listOf(first, second)) {
            val usage = assertNotNull(response.usage)
            assertTrue(usage.inputTokens > 0uL)
            assertTrue(usage.outputTokens > 0uL)
            assertEquals(usage.inputTokens + usage.outputTokens, usage.totalTokens)
        }
    }

    @Test
    fun `real API streams a tool call and consumes its result`() = runBlocking {
        val provider = OpenAILiveProvider.create()
        val prompt = calculatorPrompt()
        val first = collectAssistantEvents(provider.generate_stream(prompt).getOrThrow()).requireNoFailures("first stream")
        assertEquals(listOf<FinishReason>(FinishReason.ToolCalls), first.finishedReasons)
        val calls = first.toolCallsByIndex()
        assertEquals(setOf(0), calls.keys)
        val call = calls.getValue(0)
        assertTrue(call.id.isNotBlank())
        assertEquals("add", call.name)
        val arguments = call.arguments.jsonObject
        assertEquals(7, arguments.getValue("a").jsonPrimitive.int)
        assertEquals(11, arguments.getValue("b").jsonPrimitive.int)
        val sum = arguments.getValue("a").jsonPrimitive.int + arguments.getValue("b").jsonPrimitive.int
        assertUsage(assertNotNull(first.usage, "the tool call stream carried no usage"))

        val consumed = collectAssistantEvents(
            provider.generate_stream(prompt.copy(
                messages = prompt.messages +
                    ChatMessage.Assistant(content = first.text.takeIf { it.isNotEmpty() }, toolCalls = listOf(call)) +
                    ChatMessage.Tool(call.id, sum.toString()),
            )).getOrThrow(),
        ).requireNoFailures("second stream")
        assertEquals(listOf<FinishReason>(FinishReason.Stop), consumed.finishedReasons)
        assertEquals(18, consumed.text.trim().toIntOrNull(), "streamed answer was ${consumed.text.trim()}")
        assertTrue(consumed.toolCallsByIndex().isEmpty(), "the model called another tool after receiving the result")
        assertUsage(assertNotNull(consumed.usage, "the answer stream carried no usage"))
    }

    private fun assertUsage(usage: Usage) {
        assertTrue(usage.inputTokens > 0uL)
        assertTrue(usage.outputTokens > 0uL)
        assertEquals(usage.inputTokens + usage.outputTokens, usage.totalTokens)
    }
}
