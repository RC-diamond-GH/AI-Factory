package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.FinishReason
import com.rcdiamond.aifactory.client.llm.GenerateResponse
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ModelProvider
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.shared.SharedSessionLimits
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedSessionTitleGeneratorTest {
    private val history = listOf(ChatMessage.User("帮我合成铁锭"), ChatMessage.Assistant("已提交合成任务"))

    private fun generator(response: GenerateResponse) = SharedSessionTitleGenerator(object : ModelProvider {
        override suspend fun generate(prompt: LLMContext): GenerateResponse = response
        override suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>> =
            error("Title generation must not stream")
    })

    @Test
    fun `quotes and extra lines do not become part of the stored title`() = runBlocking {
        val title = generator(GenerateResponse(ChatMessage.Assistant("  “铁锭合成”\n这是解释  "), FinishReason.Stop))
            .generate(history)
        assertEquals("铁锭合成", title)
    }

    @Test
    fun `a long title respects the storage bound without splitting a surrogate pair`() = runBlocking {
        val prefix = "a".repeat(SharedSessionLimits.MAX_TITLE_LENGTH - 1)
        val title = generator(GenerateResponse(ChatMessage.Assistant(prefix + "\uD83D\uDE00"), FinishReason.Stop))
            .generate(history)
        assertEquals(prefix, title)
    }

    @Test
    fun `empty filtered truncated and tool responses cannot count as a successful title`() = runBlocking {
        val responses = listOf(
            GenerateResponse(ChatMessage.Assistant("  \n  "), FinishReason.Stop),
            GenerateResponse(ChatMessage.Assistant("\"\""), FinishReason.Stop),
            GenerateResponse(ChatMessage.Assistant("partial"), FinishReason.Length),
            GenerateResponse(ChatMessage.Assistant("blocked"), FinishReason.ContentFilter),
            GenerateResponse(ChatMessage.Assistant("title", listOf(
                ToolCall("call", "ae2_request_crafting", JsonObject(emptyMap())),
            )), FinishReason.Stop),
        )
        for (response in responses) {
            assertFailsWith<IOException> { generator(response).generate(history) }
        }
    }

    @Test
    fun `cancellation propagates instead of being treated as a generated title`() = runBlocking<Unit> {
        val naming = SharedSessionTitleGenerator(object : ModelProvider {
            override suspend fun generate(prompt: LLMContext): GenerateResponse = throw CancellationException("cancelled")
            override suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>> = error("unused")
        })
        assertFailsWith<CancellationException> { naming.generate(history) }
    }
}
