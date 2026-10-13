package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Records the arguments it was asked to run; does nothing else. */
private class RecordingTool(
    override val definition: ToolDefinition,
    private val answer: (JsonElement) -> JsonElement = { JsonPrimitive("ok") },
) : ClientTool {
    val received = mutableListOf<JsonElement>()

    override suspend fun execute(arguments: JsonElement): JsonElement {
        received += arguments
        return answer(arguments)
    }
}

private fun toolDefinition(name: String) =
    ToolDefinition(name, "test tool", buildJsonObject { put("type", "object") })

private fun toolCall(name: String, arguments: JsonElement = JsonObject(emptyMap()), id: String = "call_1") =
    ToolCall(id, name, arguments)

private fun errorOf(reply: ChatMessage.Tool): String {
    val observation = assertIs<JsonObject>(Json.parseToJsonElement(reply.content))
    return assertIs<JsonPrimitive>(observation["error"]).content
}

class ToolManagerTest {
    @Test
    fun `definitions keep registration order and duplicate names are rejected`() {
        val manager = ToolManager()
        manager.register(RecordingTool(toolDefinition("first")))
        manager.register(RecordingTool(toolDefinition("second")))
        assertEquals(listOf("first", "second"), manager.definitions.map { it.name })

        assertFailsWith<IllegalArgumentException> {
            manager.register(RecordingTool(toolDefinition("first")))
        }
        assertEquals(listOf("first", "second"), manager.definitions.map { it.name })
    }

    @Test
    fun `an unknown tool becomes an observation instead of a failure`() {
        runBlocking {
            val reply = ToolManager().execute(toolCall("teleport", id = "call_2"))

            assertEquals("call_2", reply.toolCallId)
            assertEquals("unknown_tool", errorOf(reply))
            val observation = assertIs<JsonObject>(Json.parseToJsonElement(reply.content))
            assertEquals("teleport", assertIs<JsonPrimitive>(observation["tool"]).content)
        }
    }

    @Test
    fun `arguments that are not an object are reported without running the tool`() {
        runBlocking {
            val tool = RecordingTool(toolDefinition("echo"))
            val manager = ToolManager()
            manager.register(tool)

            val reply = manager.execute(toolCall("echo", JsonPrimitive("not an object")))

            assertEquals("invalid_arguments", errorOf(reply))
            assertTrue(tool.received.isEmpty())
        }
    }

    @Test
    fun `expected argument failures become observations with their own message`() {
        runBlocking {
            val manager = ToolManager()
            manager.register(RecordingTool(toolDefinition("count")) { throw ToolArgumentException("count must be positive") })

            val reply = manager.execute(toolCall("count"))

            assertEquals("invalid_arguments", errorOf(reply))
        }
    }

    @Test
    fun `implementation bugs are never swallowed`() {
        val manager = ToolManager()
        manager.register(RecordingTool(toolDefinition("buggy")) { throw IllegalStateException("boom") })

        assertFailsWith<IllegalStateException> { runBlocking { manager.execute(toolCall("buggy")) } }
    }

    @Test
    fun `cancellation is propagated instead of being reported as a tool error`() {
        val manager = ToolManager()
        manager.register(RecordingTool(toolDefinition("cancelling")) { throw CancellationException("cancelled") })

        assertFailsWith<CancellationException> { runBlocking { manager.execute(toolCall("cancelling")) } }
    }
}
