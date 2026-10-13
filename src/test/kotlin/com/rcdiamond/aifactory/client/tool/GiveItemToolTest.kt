package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.GiveItemGateway
import com.rcdiamond.aifactory.client.llm.GiveItemResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Records what the tool asked the server to do without doing anything itself. */
private class RecordingGateway(
    private val result: (String, Int) -> GiveItemResult = { _, _ -> GiveItemResult(UUID.randomUUID(), true, "ok") },
) : GiveItemGateway {
    val calls = mutableListOf<Pair<String, Int>>()

    override suspend fun giveItem(itemId: String, count: Int): GiveItemResult {
        calls += itemId to count
        return result(itemId, count)
    }
}

private fun arguments(itemId: JsonElement, count: JsonElement) = buildJsonObject {
    put("item_id", itemId)
    put("count", count)
}

private fun giveItem(itemId: String, count: Int) = arguments(JsonPrimitive(itemId), JsonPrimitive(count))

/** The tool must refuse [arguments] before the gateway is ever asked. */
private fun rejection(tool: GiveItemTool, arguments: JsonElement): ToolArgumentException =
    assertFailsWith<ToolArgumentException> { runBlocking { tool.execute(arguments) } }

private fun observationOf(element: JsonElement): JsonObject = assertIs<JsonObject>(element)

class GiveItemToolTest {
    @ParameterizedTest
    @ValueSource(ints = [1, 2304])
    fun `the count boundaries are accepted`(count: Int) {
        val gateway = RecordingGateway()
        val tool = GiveItemTool(gateway)

        runBlocking {
            assertEquals(true, assertIs<JsonPrimitive>(observationOf(tool.execute(giveItem("minecraft:diamond", count)))["success"]).boolean)
        }
        assertEquals(listOf("minecraft:diamond" to count), gateway.calls)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 2305, 99999])
    fun `counts outside the boundaries are rejected before the gateway`(count: Int) {
        val gateway = RecordingGateway()

        rejection(GiveItemTool(gateway), giveItem("minecraft:diamond", count))

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `counts that are not integers are rejected`() {
        val gateway = RecordingGateway()
        val tool = GiveItemTool(gateway)

        rejection(tool, arguments(JsonPrimitive("minecraft:diamond"), JsonPrimitive("3")))
        rejection(tool, arguments(JsonPrimitive("minecraft:diamond"), JsonPrimitive(1.5)))
        rejection(tool, arguments(JsonPrimitive("minecraft:diamond"), JsonNull))
        assertTrue(gateway.calls.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Diamond", "minecraft:", ":diamond", "#minecraft:planks", "minecraft:diamond sword", "minecraft:diamond#tag"])
    fun `item ids that are not resource locations are rejected`(itemId: String) {
        val gateway = RecordingGateway()

        rejection(GiveItemTool(gateway), giveItem(itemId, 1))

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `an absurdly long item id is rejected instead of being echoed to the server`() {
        val gateway = RecordingGateway()

        rejection(GiveItemTool(gateway), giveItem("a".repeat(300), 1))

        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `parameters outside the declared shape are rejected`() {
        val gateway = RecordingGateway()
        val tool = GiveItemTool(gateway)

        rejection(tool, buildJsonObject { put("item_id", "minecraft:diamond") })
        rejection(tool, buildJsonObject { put("item_id", "minecraft:diamond"); put("count", 1); put("nbt", "{x:1}") })
        rejection(tool, arguments(JsonPrimitive(7), JsonPrimitive(1)))
        rejection(tool, arguments(JsonNull, JsonPrimitive(1)))
        rejection(tool, JsonPrimitive("minecraft:diamond"))
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `cancellation of a pending grant propagates`() {
        val tool = GiveItemTool(RecordingGateway { _, _ -> throw CancellationException("cancelled") })

        assertFailsWith<CancellationException> { runBlocking { tool.execute(giveItem("minecraft:diamond", 1)) } }
    }
}
