package com.rcdiamond.aifactory.addon.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val NETWORK_INFO = IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO
private val LIST_PARTS = IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS
private val LIST_VARIABLES = IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES

/** Counts the calls it is asked to run so a rejected request can prove it never reached the server. */
private class RecordingGateway(private val response: JsonObject) : IntegratedDynamicsToolGateway {
    val calls = mutableListOf<Pair<String, JsonObject>>()

    override suspend fun execute(operation: String, arguments: JsonObject): JsonObject {
        calls += operation to arguments
        return response
    }
}

private fun newManager(gateway: RecordingGateway): ToolManager =
    ToolManager().also { IntegratedDynamicsTools.register(it, gateway) }

private fun replyOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject {
    val reply = runBlocking { manager.execute(ToolCall("call_1", name, arguments)) }
    return assertIs<JsonObject>(Json.parseToJsonElement(assertIs<ChatMessage.Tool>(reply).content))
}

private fun failureOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject {
    val reply = replyOf(manager, name, arguments)
    assertEquals("invalid_arguments", reply.getValue("error").jsonPrimitive.content)
    assertTrue(reply.containsKey("message"), "a rejection must explain itself: $reply")

    return reply
}

class IntegratedDynamicsToolsTest {

    @Test
    fun `malformed arguments are rejected locally and never reach the gateway`() {
        val gateway = RecordingGateway(buildJsonObject { put("ok", true) })
        val manager = newManager(gateway)

        val bad = listOf(
            NETWORK_INFO to buildJsonObject { put("page", 0) }, // network info takes nothing
            NETWORK_INFO to JsonArray(listOf(JsonPrimitive("x"))), // not an object at all
            LIST_PARTS to buildJsonObject { put("page", -1) },
            LIST_PARTS to buildJsonObject { put("page", IntegratedDynamicsToolLimits.MAX_PAGE + 1) },
            LIST_PARTS to buildJsonObject { put("page_size", 0) },
            LIST_PARTS to buildJsonObject { put("page_size", IntegratedDynamicsToolLimits.MAX_PAGE_SIZE + 1) },
            LIST_PARTS to buildJsonObject {
                put("filter", "f".repeat(IntegratedDynamicsToolLimits.MAX_FILTER_LENGTH + 1))
            },
            LIST_PARTS to buildJsonObject { put("filter", 7) },
            LIST_PARTS to buildJsonObject { put("offset", 5) }, // unknown parameter
            LIST_PARTS to buildJsonObject { put("limit", 5) }, // unknown parameter
            LIST_VARIABLES to buildJsonObject { put("page", "3") }, // a numeric string is not an integer
            LIST_VARIABLES to buildJsonObject { put("page", JsonNull) },
            LIST_VARIABLES to buildJsonObject { put("page", 1.5) },
            LIST_VARIABLES to buildJsonObject { put("filter", true) },
            LIST_VARIABLES to buildJsonObject { put("sort", "name") }, // unknown parameter
        )

        for ((name, arguments) in bad) {
            failureOf(manager, name, arguments)
        }
        assertTrue(gateway.calls.isEmpty(), "the server must never see arguments the tools can reject: ${gateway.calls}")
    }

}
