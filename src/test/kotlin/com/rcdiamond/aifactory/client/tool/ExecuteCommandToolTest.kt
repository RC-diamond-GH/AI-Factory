package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.ExecuteCommandGateway
import com.rcdiamond.aifactory.client.llm.ExecuteCommandResult
import com.rcdiamond.aifactory.server.CommandLimits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Records what the tool asked the server to run without doing anything itself. */
private class RecordingCommandGateway(
    private val result: (String) -> ExecuteCommandResult =
        { ExecuteCommandResult(UUID.randomUUID(), true, 1, "ok") },
) : ExecuteCommandGateway {
    val calls = mutableListOf<String>()

    override suspend fun executeCommand(command: String): ExecuteCommandResult {
        calls += command
        return result(command)
    }
}

private fun arguments(command: JsonElement) = buildJsonObject { put("command", command) }

private fun command(command: String) = arguments(JsonPrimitive(command))

/** The tool must refuse [arguments] before the gateway is ever asked. */
private fun rejection(tool: ExecuteCommandTool, arguments: JsonElement): ToolArgumentException =
    assertFailsWith<ToolArgumentException> { runBlocking { tool.execute(arguments) } }

class ExecuteCommandToolTest {
    @ParameterizedTest
    @ValueSource(strings = ["say hi", "/say hi", "  say hi  ", "/  say hi"])
    fun `a leading slash and outer whitespace are stripped while the body stays intact`(raw: String) {
        val gateway = RecordingCommandGateway()

        runBlocking { ExecuteCommandTool(gateway).execute(command(raw)) }

        assertEquals(listOf("say hi"), gateway.calls)
    }

    @Test
    fun `inner content is preserved instead of being split on shell punctuation`() {
        val gateway = RecordingCommandGateway()

        runBlocking { ExecuteCommandTool(gateway).execute(command("execute if block ~ ~ ~ minecraft:stone run say a;b | c")) }

        assertEquals(listOf("execute if block ~ ~ ~ minecraft:stone run say a;b | c"), gateway.calls)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "/", "/   "])
    fun `blank commands are rejected before the gateway`(raw: String) {
        val gateway = RecordingCommandGateway()

        rejection(ExecuteCommandTool(gateway), command(raw))

        assertTrue(gateway.calls.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["say a\nb", "say a\rb", "say a\u0000b"])
    fun `line breaks and NUL are rejected before the gateway`(raw: String) {
        val gateway = RecordingCommandGateway()

        rejection(ExecuteCommandTool(gateway), command(raw))

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `the command length boundary is enforced on the raw text`() {
        val gateway = RecordingCommandGateway()
        val tool = ExecuteCommandTool(gateway)

        runBlocking { tool.execute(command("a".repeat(CommandLimits.MAX_COMMAND_LENGTH))) }
        assertEquals(listOf("a".repeat(CommandLimits.MAX_COMMAND_LENGTH)), gateway.calls)

        gateway.calls.clear()
        rejection(tool, command("a".repeat(CommandLimits.MAX_COMMAND_LENGTH + 1)))
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `parameters outside the declared shape are rejected`() {
        val gateway = RecordingCommandGateway()
        val tool = ExecuteCommandTool(gateway)

        rejection(tool, buildJsonObject { })
        rejection(tool, buildJsonObject { put("command", "say hi"); put("request_id", "x") })
        rejection(tool, arguments(JsonPrimitive(7)))
        rejection(tool, arguments(JsonPrimitive(true)))
        rejection(tool, arguments(JsonNull))
        rejection(tool, JsonPrimitive("say hi"))
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `cancellation of a pending command propagates`() {
        val tool = ExecuteCommandTool(RecordingCommandGateway { throw CancellationException("cancelled") })

        assertFailsWith<CancellationException> { runBlocking { tool.execute(command("say hi")) } }
    }
}
