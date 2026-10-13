package com.rcdiamond.aifactory.addon.ae2.client.tool

import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val FINGERPRINT = "cd".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH / 2)

/** Records the operations it is asked to perform and answers with a fixed server reply. */
private class RecordingGateway(private val response: JsonObject) {
    val calls = mutableListOf<Pair<String, JsonObject>>()

    val gateway: suspend (String, JsonObject) -> JsonObject = { operation, arguments ->
        calls += operation to arguments
        response
    }
}

private fun newManager(gateway: RecordingGateway): ToolManager =
    ToolManager().also { Ae2Tools.register(it, gateway.gateway) }

private fun replyOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject {
    val reply = runBlocking { manager.execute(ToolCall("call_1", name, arguments)) }
    return assertIs<JsonObject>(Json.parseToJsonElement(assertIs<ChatMessage.Tool>(reply).content))
}

private fun failureOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject =
    replyOf(manager, name, arguments).also { assertEquals("invalid_arguments", it.getValue("error").jsonPrimitive.content) }

class Ae2ToolsTest {

    @Test
    fun `malformed arguments are rejected locally and never reach the gateway`() {
        val gateway = RecordingGateway(buildJsonObject { put("ok", true) })
        val manager = newManager(gateway)

        failureOf(manager, Ae2ToolLimits.OPERATION_READ_INVENTORY, buildJsonObject { put("page", -1) })
        failureOf(manager, Ae2ToolLimits.OPERATION_READ_INVENTORY, buildJsonObject { put("nbt", "x") })
        failureOf(manager, Ae2ToolLimits.OPERATION_ITEM_DETAILS, buildJsonObject { put("key", "not-a-fingerprint") })
        failureOf(manager, Ae2ToolLimits.OPERATION_ITEM_DETAILS, JsonObject(emptyMap()))
        failureOf(
            manager,
            Ae2ToolLimits.OPERATION_CRAFTING_PLAN,
            buildJsonObject {
                put("key", FINGERPRINT)
                put("amount", 0)
            },
        )
        failureOf(manager, Ae2ToolLimits.OPERATION_ITEM_DETAILS, JsonArray(listOf(JsonPrimitive(FINGERPRINT))))

        assertTrue(gateway.calls.isEmpty(), "the server must never see arguments the client could reject")
    }

    @Test
    fun `a recognized key outside the declared shape is still rejected`() {
        val gateway = RecordingGateway(buildJsonObject { put("ok", true) })
        val manager = newManager(gateway)

        val failure = failureOf(
            manager,
            Ae2ToolLimits.OPERATION_ITEM_DETAILS,
            buildJsonObject {
                put("key", FINGERPRINT)
                put("path", "tag..broken")
            },
        )
        assertTrue(failure.containsKey("message"))
        assertTrue(gateway.calls.isEmpty())
    }
}
