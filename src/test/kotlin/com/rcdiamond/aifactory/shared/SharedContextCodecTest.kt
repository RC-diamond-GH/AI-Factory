package com.rcdiamond.aifactory.shared

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.GlobalPrompt
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SharedContextCodecTest {

    private fun richContext(): LLMContext = LLMContext(
        systemPrompt = "You are a factory agent.\nBe terse.",
        globalPrompt = listOf(GlobalPrompt("notes.md", "iron is cheap")),
        tools = listOf(
            ToolDefinition(
                "ae2_read_inventory",
                "Lists the terminal inventory",
                buildJsonObject {
                    put("type", "object")
                    put(
                        "properties",
                        buildJsonObject {
                            put("page", buildJsonObject { put("type", "integer") })
                            put("password", buildJsonObject { put("type", "string") })
                        },
                    )
                },
            ),
        ),
        messages = listOf(
            ChatMessage.User("What is in the drive?"),
            ChatMessage.Assistant(
                content = null,
                toolCalls = listOf(ToolCall("call-1", "ae2_read_inventory", buildJsonObject { put("page", 1) })),
            ),
            ChatMessage.Tool("call-1", """{"ok":true,"items":[]}"""),
            ChatMessage.Assistant("Nothing yet."),
        ),
    )

    @Test
    fun `encode and decode are exact inverses`() {
        val original = richContext()
        val encoded = SharedContextCodec.encode(original)
        val decoded = SharedContextCodec.decode(encoded)

        assertEquals(original, decoded)
        assertEquals(encoded, SharedContextCodec.encode(decoded))
    }

    @Test
    fun `an assistant message without content stays distinct from an empty one`() {
        val without = LLMContext("s", messages = listOf(ChatMessage.User("u"), ChatMessage.Assistant(content = null)))
        val empty = LLMContext("s", messages = listOf(ChatMessage.User("u"), ChatMessage.Assistant(content = "")))

        assertEquals(without, SharedContextCodec.decode(SharedContextCodec.encode(without)))
        assertEquals(empty, SharedContextCodec.decode(SharedContextCodec.encode(empty)))
        assertNotEquals(SharedContextCodec.hash(SharedContextCodec.encode(without)), SharedContextCodec.hash(SharedContextCodec.encode(empty)))
    }

    @Test
    fun `the hash ignores object key order but keeps array order`() {
        val first = buildJsonObject {
            put("a", 1)
            put("b", 2)
        }
        val reversed = JsonObject(linkedMapOf("b" to JsonPrimitive(2), "a" to JsonPrimitive(1)))
        assertEquals(SharedContextCodec.hash(first), SharedContextCodec.hash(reversed))

        val ascending = buildJsonObject { put("list", buildJsonArray { add(1); add(2) }) }
        val descending = buildJsonObject { put("list", buildJsonArray { add(2); add(1) }) }
        assertNotEquals(SharedContextCodec.hash(ascending), SharedContextCodec.hash(descending))
    }

    @Test
    fun `the hash is stable across a decode and re-encode cycle`() {
        val encoded = SharedContextCodec.encode(richContext())
        assertEquals(SharedContextCodec.hash(encoded), SharedContextCodec.hash(SharedContextCodec.decode(encoded).let(SharedContextCodec::encode)))
    }


    @Test
    fun `charge is a ceiling division by 32`() {
        assertEquals(0L, SharedSessionLimits.chargeFor(0))
        assertEquals(1L, SharedSessionLimits.chargeFor(1))
        assertEquals(1L, SharedSessionLimits.chargeFor(32))
        assertEquals(2L, SharedSessionLimits.chargeFor(33))
        assertEquals(32768L, SharedSessionLimits.chargeFor(1024L * 1024))
    }


    @Test
    fun `decode rejects a tool result that answers no call`() {
        val context = LLMContext("s", messages = listOf(ChatMessage.User("u"), ChatMessage.Tool("missing", "x")))
        assertFailsWith<IllegalArgumentException> { SharedContextCodec.decode(SharedContextCodec.encode(context)) }
    }

    @Test
    fun `decode rejects a call answered twice`() {
        val broken = buildJsonObject {
            put("systemPrompt", "")
            put(
                "messages",
                buildJsonArray {
                    add(buildJsonObject { put("role", "user"); put("content", "u") })
                    add(
                        buildJsonObject {
                            put("role", "assistant")
                            put(
                                "toolCalls",
                                buildJsonArray {
                                    add(buildJsonObject { put("id", "c1"); put("name", "n"); put("arguments", buildJsonObject {}) })
                                },
                            )
                        },
                    )
                    add(buildJsonObject { put("role", "tool"); put("toolCallId", "c1"); put("content", "r") })
                    add(buildJsonObject { put("role", "tool"); put("toolCallId", "c1"); put("content", "r") })
                },
            )
        }
        assertFailsWith<IllegalArgumentException> { SharedContextCodec.decode(broken) }
    }

    @Test
    fun `decode rejects a message that follows unanswered tool calls`() {
        val encoded = SharedContextCodec.encode(richContext())
        val messages = (encoded["messages"] as JsonArray).toList()
        // Drop the tool result (index 2) so the user message that follows arrives while calls are pending.
        val broken = buildJsonObject {
            put("systemPrompt", encoded["systemPrompt"]!!)
            put("messages", JsonArray(listOf(messages[0], messages[1], messages[3])))
        }
        assertFailsWith<IllegalArgumentException> { SharedContextCodec.decode(broken) }
    }

    @Test
    fun `decode rejects an unknown message role`() {
        val broken = buildJsonObject {
            put("systemPrompt", "")
            put("messages", buildJsonArray { add(buildJsonObject { put("role", "system"); put("content", "x") }) })
        }
        assertFailsWith<IllegalArgumentException> { SharedContextCodec.decode(broken) }
    }


    @Test
    fun `decode rejects a document nested past the depth limit`() {
        var nested: JsonObject = buildJsonObject { put("leaf", 1) }
        repeat(SharedContextCodec.MAX_DEPTH + 2) {
            nested = buildJsonObject { put("nested", nested) }
        }
        val broken = buildJsonObject {
            put("systemPrompt", "")
            put("extra", nested)
            put("messages", buildJsonArray {})
        }
        assertFailsWith<IllegalArgumentException> { SharedContextCodec.decode(broken) }
    }
}
