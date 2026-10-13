package com.rcdiamond.aifactory.client.ui

import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import com.rcdiamond.aifactory.client.llm.ChatEntry
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * A stand-in for `Component.translatable(key, *args).string` that keeps the key and every argument
 * visible, so the tests describe the summary's structure and never pin translated wording.
 */
private val probe: (String, Array<out Any>) -> String = { key, args ->
    if (args.isEmpty()) key else key + "(" + args.joinToString("|") + ")"
}

private fun keyOf(line: String): String = line.substringBefore('(')

private fun argsOf(line: String): List<String> {
    val open = line.indexOf('(')
    if (open < 0) return emptyList()
    return line.substring(open + 1, line.length - 1).split("|")
}

/** Anything in the payload that must never reach the chat: values, raw JSON and echoed arguments. */
private const val MARKER = "SECRET_MARKER"

private val leaks = listOf(MARKER, "{", "}", "\"", "page_size", "fingerprint_of")

private fun assertNoLeak(line: String) {
    for (leak in leaks) assertFalse(line.contains(leak), "summary leaked '$leak': $line")
}

private fun inventoryReply(items: Int, amount: Long, page: Int, pageCount: Int): JsonObject = buildJsonObject {
    put("ok", true)
    put("filter", MARKER)
    put("page", page)
    put("page_size", 20)
    put("page_count", pageCount)
    put("total_items", items)
    put("total_amount", amount)
    put("has_more", page + 1 < pageCount)
    putJsonArray("items") {
        add(
            buildJsonObject {
                put("key", "$MARKER-key")
                put("id", "$MARKER:id")
                put("name", "$MARKER name")
                put("count", amount)
                put("craftable", true)
            },
        )
    }
}

private fun itemDetailsReply(found: Boolean): JsonObject = buildJsonObject {
    put("ok", true)
    put("key", "$MARKER-key")
    put("name", "Iron Ingot")
    put("count", 64)
    put("craftable", true)
    putJsonObject("nbt") {
        put("present", true)
        put("path", MARKER)
        put("found", found)
        put("value", "$MARKER value")
    }
}

private fun craftingPlanReply(): JsonObject = buildJsonObject {
    put("ok", true)
    put("submitted", false)
    put("name", "Crafting Table")
    put("amount", 4)
    put("bytes", 1234)
    put("missing_item_kinds", 3)
    putJsonArray("missing_items") {
        add(buildJsonObject { put("name", MARKER) })
    }
    putJsonObject("final_output") { put("name", MARKER) }
}

private fun requestCraftingReply(): JsonObject = buildJsonObject {
    put("ok", true)
    put("submitted", true)
    put("name", "Crafting Table")
    put("amount", 4)
    put("bytes", 1234)
}

private fun patternViewReply(): JsonObject = buildJsonObject {
    put("ok", true)
    put("action", "view")
    put("page", 0)
    put("page_size", 10)
    put("page_count", 3)
    put("total_patterns", 25)
    put("has_more", true)
    put("blank_patterns", 7)
    putJsonArray("patterns") { add(buildJsonObject { put("name", MARKER) }) }
    put("note", MARKER)
}

private fun patternInstallReply(): JsonObject = buildJsonObject {
    put("ok", true)
    put("action", "encode_install")
    put("installed", true)
    put("mode", "processing")
    put("blank_patterns_remaining", 6)
    put("expected", MARKER)
    put("slot", 3)
}

class ToolUseSummaryTest {

    // ------------------------------------------------------------------ success

    @Test
    fun `an inventory read summarizes counts and paging without leaking the page contents`() {
        val line = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_READ_INVENTORY,
            inventoryReply(items = 120, amount = 45_000, page = 1, pageCount = 6).toString(),
            probe,
        )

        assertEquals("ai_factory.tool.summary.inventory", keyOf(line))
        assertEquals(
            listOf("ai_factory.tool.name.ae2_read_inventory", "120", "45000", "2", "6"),
            argsOf(line),
        )
        assertNoLeak(line)
    }

    @Test
    fun `an empty inventory still reports a usable page range`() {
        val line = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_READ_INVENTORY,
            inventoryReply(items = 0, amount = 0, page = 0, pageCount = 0).toString(),
            probe,
        )

        assertEquals(listOf("ai_factory.tool.name.ae2_read_inventory", "0", "0", "1", "1"), argsOf(line))
    }

    @Test
    fun `item details report the stored amount and whether the nbt path resolved`() {
        val found = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_ITEM_DETAILS, itemDetailsReply(true).toString(), probe)
        val missing = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_ITEM_DETAILS, itemDetailsReply(false).toString(), probe)

        assertEquals("ai_factory.tool.summary.item_details", keyOf(found))
        assertEquals(listOf("ai_factory.tool.name.ae2_item_details", "Iron Ingot", "64"), argsOf(found))
        assertNotEquals(keyOf(found), keyOf(missing), "a result the tool could not resolve must read differently")
        assertNoLeak(found)
        assertNoLeak(missing)
    }

    @Test
    fun `a crafting plan reports the shortfall and a submitted job reports the amount`() {
        val plan = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_CRAFTING_PLAN, craftingPlanReply().toString(), probe)
        val submitted = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_REQUEST_CRAFTING, requestCraftingReply().toString(), probe)

        assertEquals("ai_factory.tool.summary.crafting_plan", keyOf(plan))
        assertEquals(listOf("ai_factory.tool.name.ae2_crafting_plan", "Crafting Table", "4", "3"), argsOf(plan))
        assertEquals("ai_factory.tool.summary.crafting_submitted", keyOf(submitted))
        assertEquals(listOf("ai_factory.tool.name.ae2_request_crafting", "Crafting Table", "4"), argsOf(submitted))
        assertNotEquals(keyOf(plan), keyOf(submitted), "a plan and a real job must be distinguishable")
        assertNoLeak(plan)
        assertNoLeak(submitted)
    }

    @Test
    fun `pattern operations report the scan page and the install result`() {
        val view = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_MANAGE_PATTERNS, patternViewReply().toString(), probe)
        val install = ToolUseSummary.finished(Ae2ToolLimits.OPERATION_MANAGE_PATTERNS, patternInstallReply().toString(), probe)

        assertEquals("ai_factory.tool.summary.patterns_view", keyOf(view))
        assertEquals(listOf("ai_factory.tool.name.ae2_manage_patterns", "25", "7", "1", "3"), argsOf(view))
        assertEquals("ai_factory.tool.summary.pattern_installed", keyOf(install))
        assertEquals(listOf("ai_factory.tool.name.ae2_manage_patterns", "processing", "6"), argsOf(install))
        assertNoLeak(view)
        assertNoLeak(install)
    }

    @Test
    fun `game actions report result scalars and never the request or its output`() {
        val given = ToolUseSummary.finished(
            "give_item",
            buildJsonObject {
                put("item_id", "$MARKER:id")
                put("count", 64)
                put("success", true)
                put("message", "Gave 64 x $MARKER:id")
            }.toString(),
            probe,
        )
        val command = ToolUseSummary.finished(
            "execute_command",
            buildJsonObject {
                put("command", "$MARKER command")
                put("success", true)
                put("result_code", 1)
                put("message", "$MARKER output")
                put("output_truncated", false)
            }.toString(),
            probe,
        )
        val truncated = ToolUseSummary.finished(
            "execute_command",
            buildJsonObject {
                put("command", "$MARKER command")
                put("success", true)
                put("result_code", 1)
                put("message", "$MARKER output")
                put("output_truncated", true)
            }.toString(),
            probe,
        )

        assertEquals(listOf("ai_factory.tool.name.give_item", "64"), argsOf(given))
        assertEquals(listOf("ai_factory.tool.name.execute_command", "1"), argsOf(command))
        assertNotEquals(keyOf(command), keyOf(truncated), "a truncated output must be visible as such")
        assertNoLeak(given)
        assertNoLeak(command)
    }

    @Test
    fun `an unknown tool is labelled by its name and never dumped`() {
        val line = ToolUseSummary.finished("custom_tool", buildJsonObject { put("ok", true) }.toString(), probe)

        assertEquals("ai_factory.tool.summary.completed", keyOf(line))
        assertEquals(listOf("ai_factory.tool.name.other(custom_tool)"), argsOf(line))
    }

    // -------------------------------------------------------------------- errors

    @Test
    fun `reported error codes become distinct failure reasons`() {
        val reasons = listOf("unknown_tool", "invalid_arguments", "timeout", "interrupted", "unknown_key", "install_failed")
            .map { code ->
                val line = ToolUseSummary.finished(
                    "ae2_read_inventory",
                    buildJsonObject {
                        put("tool", "ae2_read_inventory")
                        put("error", code)
                        put("message", "$MARKER message for $code")
                    }.toString(),
                    probe,
                )
                assertEquals("ai_factory.tool.summary.failed", keyOf(line))
                assertNoLeak(line)
                argsOf(line)[1]
            }

        assertEquals(reasons.size, reasons.toSet().size, "every error code must read differently: $reasons")
    }

    @Test
    fun `an ae2 failure keeps its code and its scalar detail without the server message`() {
        val missing = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_REQUEST_CRAFTING,
            buildJsonObject {
                put("ok", false)
                put("submitted", false)
                put("error", "missing_items")
                put("message", "$MARKER server message")
                put("missing_item_kinds", 5)
            }.toString(),
            probe,
        )
        val rejected = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_REQUEST_CRAFTING,
            buildJsonObject {
                put("ok", false)
                put("submitted", false)
                put("error", "crafting_rejected")
                put("reason", "NO_SUITABLE_CPU_FOUND")
                put("message", "$MARKER server message")
            }.toString(),
            probe,
        )
        val exotic = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_ITEM_DETAILS,
            buildJsonObject {
                put("ok", false)
                put("error", "brand_new_code")
                put("message", "$MARKER server message")
            }.toString(),
            probe,
        )

        val bare = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_ITEM_DETAILS,
            buildJsonObject {
                put("ok", false)
                put("message", "$MARKER server message")
            }.toString(),
            probe,
        )

        assertEquals("ai_factory.tool.error.missing_items(5)", argsOf(missing)[1])
        assertEquals("ai_factory.tool.error.crafting_rejected(NO_SUITABLE_CPU_FOUND)", argsOf(rejected)[1])
        assertEquals("ai_factory.tool.error.code(brand_new_code)", argsOf(exotic)[1])
        assertEquals("ai_factory.tool.error.other", argsOf(bare)[1])
        listOf(missing, rejected, exotic, bare).forEach(::assertNoLeak)
    }

    @Test
    fun `a refused game action is a failure and never echoes the server message`() {
        val line = ToolUseSummary.finished(
            "give_item",
            buildJsonObject {
                put("item_id", "$MARKER:id")
                put("count", 64)
                put("success", false)
                put("message", "Unknown item: $MARKER:id")
            }.toString(),
            probe,
        )
        val command = ToolUseSummary.finished(
            "execute_command",
            buildJsonObject {
                put("command", "$MARKER command")
                put("success", false)
                put("result_code", 0)
                put("message", "$MARKER output")
                put("output_truncated", false)
            }.toString(),
            probe,
        )

        assertEquals("ai_factory.tool.summary.failed", keyOf(line))
        assertEquals("ai_factory.tool.error.give_item_failed", argsOf(line)[1])
        assertEquals("ai_factory.tool.error.command_failed(0)", argsOf(command)[1])
        assertNoLeak(line)
        assertNoLeak(command)
    }

    @Test
    fun `a success and a failure of the same tool read differently`() {
        val ok = ToolUseSummary.finished("give_item", buildJsonObject { put("success", true); put("count", 1) }.toString(), probe)
        val bad = ToolUseSummary.finished("give_item", buildJsonObject { put("success", false); put("count", 1) }.toString(), probe)

        assertEquals("ai_factory.tool.summary.give_item", keyOf(ok))
        assertEquals("ai_factory.tool.summary.failed", keyOf(bad))
    }

    // ----------------------------------------------------------------- unreadable

    @Test
    fun `text, malformed json, arrays and empty results are reported without being copied`() {
        val payloads = listOf(
            "a plain text reply containing $MARKER",
            "{\"ok\": true, broken",
            "",
            JsonArray(listOf(JsonPrimitive("$MARKER"))).toString(),
            JsonPrimitive("$MARKER").toString(),
        )

        for (payload in payloads) {
            val line = ToolUseSummary.finished("ae2_read_inventory", payload, probe)
            assertEquals("ai_factory.tool.summary.unreadable", keyOf(line), "payload: $payload")
            assertEquals(listOf("ai_factory.tool.name.ae2_read_inventory"), argsOf(line))
            assertNoLeak(line)
        }
    }

    @Test
    fun `even a hostile result stays bounded`() {
        val bounded = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_ITEM_DETAILS,
            buildJsonObject {
                put("ok", true)
                put("name", "n".repeat(500))
                put("count", 1)
                putJsonObject("nbt") { put("found", true) }
            }.toString(),
            probe,
        )
        val longer = ToolUseSummary.finished(
            Ae2ToolLimits.OPERATION_ITEM_DETAILS,
            buildJsonObject {
                put("ok", true)
                put("name", "n".repeat(5000))
                put("count", 1)
                putJsonObject("nbt") { put("found", true) }
            }.toString(),
            probe,
        )

        assertEquals(bounded.length, longer.length, "the summary must not grow with the payload")
        assertTrue(bounded.length <= 256, "summary is too long (${bounded.length}): $bounded")
        assertTrue(argsOf(bounded)[1].endsWith("\u2026"))
    }

    @Test
    fun `every ae2 success reply stays within the chat bound`() {
        val lines = listOf(
            Ae2ToolLimits.OPERATION_READ_INVENTORY to inventoryReply(120, 45_000, 1, 6),
            Ae2ToolLimits.OPERATION_ITEM_DETAILS to itemDetailsReply(true),
            Ae2ToolLimits.OPERATION_CRAFTING_PLAN to craftingPlanReply(),
            Ae2ToolLimits.OPERATION_REQUEST_CRAFTING to requestCraftingReply(),
            Ae2ToolLimits.OPERATION_MANAGE_PATTERNS to patternViewReply(),
            Ae2ToolLimits.OPERATION_MANAGE_PATTERNS to patternInstallReply(),
        )

        for ((name, reply) in lines) {
            val line = ToolUseSummary.finished(name, reply.toString(), probe)
            assertTrue(line.length <= 256, "$name produced $line")
            assertNoLeak(line)
        }
    }

    // -------------------------------------------------------------------- history

    @Test
    fun `stored history keeps user and assistant text and folds each call into one summary`() {
        val messages = listOf<ChatMessage>(
            ChatMessage.User("hello $MARKER"),
            ChatMessage.Assistant("working", listOf(ToolCall("c1", "ae2_read_inventory", JsonObject(mapOf("filter" to JsonPrimitive(MARKER)))))),
            ChatMessage.Tool("c1", inventoryReply(120, 45_000, 1, 6).toString()),
            ChatMessage.Assistant("done"),
        )

        val entries = ToolUseSummary.history(messages, probe)

        assertEquals(listOf("user", "assistant", "tool", "assistant"), entries.map(ChatEntry::speaker))
        assertEquals("hello $MARKER", entries[0].text)
        assertEquals("working", entries[1].text)
        assertEquals("done", entries[3].text)
        assertEquals("ai_factory.tool.summary.inventory", keyOf(entries[2].text))
    }

    @Test
    fun `stored history never shows the recorded arguments`() {
        val messages = listOf<ChatMessage>(
            ChatMessage.User("do it"),
            ChatMessage.Assistant(
                null,
                listOf(ToolCall("c1", "ae2_item_details", JsonObject(mapOf("key" to JsonPrimitive("$MARKER-key"))))),
            ),
            ChatMessage.Tool("c1", itemDetailsReply(true).toString()),
        )

        val entries = ToolUseSummary.history(messages, probe)

        assertEquals(2, entries.size)
        for (entry in entries) assertFalse(entry.text.contains(MARKER), "history leaked the request: ${entry.text}")
    }

    @Test
    fun `a call whose result never arrived is reported as unanswered instead of finished`() {
        val messages = listOf<ChatMessage>(
            ChatMessage.Assistant(null, listOf(ToolCall("c1", "ae2_read_inventory", JsonObject(emptyMap())))),
        )

        val entries = ToolUseSummary.history(messages, probe)

        assertEquals(1, entries.size)
        assertEquals("tool", entries[0].speaker)
        assertEquals("ai_factory.tool.summary.no_result", keyOf(entries[0].text))
        assertEquals(listOf("ai_factory.tool.name.ae2_read_inventory"), argsOf(entries[0].text))
    }

    @Test
    fun `an interrupted call is reported differently from one that never started`() {
        val interrupted = listOf<ChatMessage>(
            ChatMessage.Assistant(null, listOf(ToolCall("c1", "ae2_read_inventory", JsonObject(emptyMap())))),
            ChatMessage.Tool("c1", "{\"tool\":\"ae2_read_inventory\",\"error\":\"interrupted\",\"message\":\"$MARKER\"}"),
        )
        val neverRan = listOf<ChatMessage>(
            ChatMessage.Assistant(
                null,
                listOf(
                    ToolCall("c1", "ae2_read_inventory", JsonObject(emptyMap())),
                    ToolCall("c2", "give_item", JsonObject(emptyMap())),
                ),
            ),
            ChatMessage.Tool("c1", "{\"tool\":\"ae2_read_inventory\",\"error\":\"interrupted\",\"message\":\"$MARKER\"}"),
        )

        assertEquals("ai_factory.tool.error.interrupted", argsOf(ToolUseSummary.history(interrupted, probe)[0].text)[1])
        val entries = ToolUseSummary.history(neverRan, probe)
        assertEquals(2, entries.size)
        assertEquals("ai_factory.tool.summary.no_result", keyOf(entries[1].text))
        assertNoLeak(entries[1].text)
    }

    @Test
    fun `a result without a recorded call is still reported truthfully`() {
        val orphan = listOf<ChatMessage>(
            ChatMessage.Tool("gone", "{\"tool\":\"ae2_read_inventory\",\"error\":\"timeout\",\"message\":\"$MARKER\"}"),
            ChatMessage.Tool("unknown", "not json $MARKER"),
        )

        val entries = ToolUseSummary.history(orphan, probe)

        assertEquals(2, entries.size)
        assertEquals("ai_factory.tool.summary.failed", keyOf(entries[0].text))
        assertEquals("ai_factory.tool.error.timeout", argsOf(entries[0].text)[1])
        assertEquals("ai_factory.tool.summary.unreadable", keyOf(entries[1].text))
        for (entry in entries) assertNoLeak(entry.text)
    }

    @Test
    fun `stored history is only read, never rewritten`() {
        val messages = arrayListOf<ChatMessage>(
            ChatMessage.User("hello"),
            ChatMessage.Assistant("working", listOf(ToolCall("c1", "give_item", JsonObject(emptyMap())))),
            ChatMessage.Tool("c1", buildJsonObject { put("success", true); put("count", 3) }.toString()),
        )
        val before = messages.toList()

        ToolUseSummary.history(messages, probe)

        assertEquals(before, messages)
    }

    @Test
    fun `a long conversation keeps the recorded order`() {
        val messages = buildList<ChatMessage> {
            for (index in 1..3) {
                add(ChatMessage.User("u$index"))
                add(ChatMessage.Assistant("a$index", listOf(ToolCall("c$index", "custom_tool", JsonObject(emptyMap())))))
                add(ChatMessage.Tool("c$index", buildJsonObject { put("ok", true) }.toString()))
            }
        }

        val speakers = ToolUseSummary.history(messages, probe).map(ChatEntry::speaker)

        assertEquals(listOf("user", "assistant", "tool", "user", "assistant", "tool", "user", "assistant", "tool"), speakers)
    }

}
