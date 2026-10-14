package com.rcdiamond.aifactory.addon.jei.client.tool

import com.rcdiamond.aifactory.addon.jei.client.JeiIngredientQuery
import com.rcdiamond.aifactory.addon.jei.client.JeiQueryGateway
import com.rcdiamond.aifactory.addon.jei.client.JeiQueryService
import com.rcdiamond.aifactory.addon.jei.client.JeiRecipeQuery
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.tool.ToolManager
import com.rcdiamond.aifactory.client.ui.ToolUseSummary
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
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A backend that answers every query with one marker object, so a reply says who served it. */
private class StubService(private val tag: String) : JeiQueryService {
    val calls = mutableListOf<String>()

    override fun findIngredients(query: JeiIngredientQuery): JsonObject {
        calls += JeiToolLimits.OPERATION_FIND_INGREDIENTS
        return reply()
    }

    override fun queryRecipes(query: JeiRecipeQuery): JsonObject {
        calls += JeiToolLimits.OPERATION_QUERY_RECIPES
        return reply()
    }

    private fun reply(): JsonObject = buildJsonObject {
        put("ok", true)
        put("tag", tag)
    }
}

private fun newManager(gateway: JeiQueryService): ToolManager =
    ToolManager().also { JeiTools.register(it, gateway) }

private fun replyOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject {
    val reply = runBlocking { manager.execute(ToolCall("call_1", name, arguments)) }
    return assertIs<JsonObject>(Json.parseToJsonElement(assertIs<ChatMessage.Tool>(reply).content))
}

private fun errorOf(manager: ToolManager, name: String, arguments: JsonElement): String =
    replyOf(manager, name, arguments).getValue("error").jsonPrimitive.content

private fun failureOf(manager: ToolManager, name: String, arguments: JsonElement): JsonObject {
    val reply = replyOf(manager, name, arguments)
    assertEquals("invalid_arguments", reply.getValue("error").jsonPrimitive.content)
    assertTrue(reply.containsKey("message"), "a rejection must explain itself: $reply")
    return reply
}

private val SEARCH = JeiToolLimits.OPERATION_FIND_INGREDIENTS
private val RECIPES = JeiToolLimits.OPERATION_QUERY_RECIPES

class JeiToolsTest {

    // ---------------------------------------------------------------- parameters

    @Test
    fun `malformed arguments are rejected locally and never reach the gateway`() {
        val gateway = StubService("stub")
        val manager = newManager(gateway)

        val bad = listOf(
            SEARCH to JsonObject(emptyMap()), // filter is required
            SEARCH to buildJsonObject { put("filter", "   ") }, // blank after trimming
            SEARCH to buildJsonObject { put("filter", "f".repeat(JeiToolLimits.MAX_FILTER_LENGTH + 1)) },
            SEARCH to buildJsonObject { put("filter", "x"); put("page", -1) },
            SEARCH to buildJsonObject { put("filter", "x"); put("page", JeiToolLimits.MAX_PAGE + 1) },
            SEARCH to buildJsonObject { put("filter", "x"); put("page", 1.5) }, // a fraction is not an integer
            SEARCH to buildJsonObject { put("filter", "x"); put("page", "3") }, // a numeric string is not either
            SEARCH to buildJsonObject { put("filter", "x"); put("page", JsonNull) }, // an explicit null is not either
            SEARCH to buildJsonObject { put("filter", "x"); put("page", true) },
            SEARCH to buildJsonObject { put("filter", "x"); put("page_size", 0) },
            SEARCH to buildJsonObject { put("filter", "x"); put("page_size", JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE + 1) },
            SEARCH to buildJsonObject { put("filter", "x"); put("filter_extra", "y") }, // unknown parameter
            SEARCH to JsonArray(listOf(JsonPrimitive("x"))), // not an object at all
            RECIPES to JsonObject(emptyMap()), // neither key nor id
            RECIPES to buildJsonObject { put("ingredient_key", "k"); put("ingredient_id", "minecraft:stone") },
            RECIPES to buildJsonObject { put("ingredient_key", "k"); put("ingredient_type", "item") },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("ingredient_type", "block") },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("direction", "food") },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("direction", 3) },
            RECIPES to buildJsonObject { put("ingredient_id", "STONE") }, // resource ids are lowercase
            RECIPES to buildJsonObject { put("ingredient_id", "a:b:c") },
            RECIPES to buildJsonObject { put("ingredient_id", "invalid/namespace:stone") },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("category_id", "Crafting") },
            RECIPES to buildJsonObject { put("ingredient_key", "k".repeat(JeiToolLimits.MAX_KEY_LENGTH + 1)) },
            RECIPES to buildJsonObject { put("ingredient_id", "a".repeat(JeiToolLimits.MAX_ID_LENGTH + 1)) },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("page", -1) },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("page_size", JeiToolLimits.MAX_RECIPE_PAGE_SIZE + 1) },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("nbt", "x") },
            RECIPES to buildJsonObject { put("ingredient_id", "minecraft:stone"); put("ingredient_key", JsonNull) },
        )

        for ((name, arguments) in bad) {
            failureOf(manager, name, arguments)
        }
        assertTrue(gateway.calls.isEmpty(), "the backend must never see arguments the tools can reject: ${gateway.calls}")
    }

    @Test
    fun `exactly one of key or id is required and the type only pairs with an id`() {
        val gateway = StubService("stub")
        val manager = newManager(gateway)

        val neither = failureOf(manager, RECIPES, buildJsonObject { put("page", 0) })
        val both = failureOf(
            manager,
            RECIPES,
            buildJsonObject {
                put("ingredient_key", "k")
                put("ingredient_id", "minecraft:stone")
            },
        )
        // A type is meaningless for an opaque runtime key, so it is refused even with a valid key.
        val typedKey = failureOf(
            manager,
            RECIPES,
            buildJsonObject {
                put("ingredient_key", "k")
                put("ingredient_type", "fluid")
            },
        )
        val reasons = listOf(neither, both, typedKey).map { it.getValue("message").jsonPrimitive.content }
        assertEquals(reasons.size, reasons.toSet().size, "each violation must explain itself: $reasons")

        // A type is accepted together with an id.
        val accepted = replyOf(
            manager,
            RECIPES,
            buildJsonObject {
                put("ingredient_id", "minecraft:water")
                put("ingredient_type", "fluid")
            },
        )
        assertEquals(true, accepted.getValue("ok").jsonPrimitive.content.toBoolean())
        assertEquals(listOf(RECIPES), gateway.calls)
    }

    @Test
    fun `the declared bounds are inclusive`() {
        val gateway = StubService("stub")
        val manager = newManager(gateway)

        val search = replyOf(
            manager,
            SEARCH,
            buildJsonObject {
                put("filter", "f".repeat(JeiToolLimits.MAX_FILTER_LENGTH))
                put("page", JeiToolLimits.MAX_PAGE)
                put("page_size", JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE)
            },
        )
        val recipes = replyOf(
            manager,
            RECIPES,
            buildJsonObject {
                put("ingredient_id", "minecraft:" + "a".repeat(JeiToolLimits.MAX_ID_LENGTH - "minecraft:".length))
                put("page", JeiToolLimits.MAX_PAGE)
                put("page_size", JeiToolLimits.MAX_RECIPE_PAGE_SIZE)
            },
        )

        assertEquals(true, search.getValue("ok").jsonPrimitive.content.toBoolean())
        assertEquals(true, recipes.getValue("ok").jsonPrimitive.content.toBoolean())

        failureOf(
            manager,
            SEARCH,
            buildJsonObject { put("filter", "f".repeat(JeiToolLimits.MAX_FILTER_LENGTH + 1)) },
        )
        failureOf(
            manager,
            RECIPES,
            buildJsonObject {
                put("ingredient_id", "a".repeat(JeiToolLimits.MAX_ID_LENGTH + 1))
            },
        )
    }

    // ------------------------------------------------------------------ lifecycle

    @Test
    fun `without an installed backend both tools answer jei_not_ready`() {
        // Installing and uninstalling a throwaway backend leaves the shared gateway empty whatever
        // another test may have left behind.
        val guard = StubService("guard")
        JeiQueryGateway.install(guard)
        JeiQueryGateway.uninstall(guard)

        val manager = ToolManager().also { JeiTools.register(it) }

        assertEquals("jei_not_ready", errorOf(manager, SEARCH, buildJsonObject { put("filter", "iron") }))
        assertEquals("jei_not_ready", errorOf(manager, RECIPES, buildJsonObject { put("ingredient_id", "minecraft:stone") }))
    }

    @Test
    fun `a stale uninstall does not remove the current backend`() {
        val first = StubService("first")
        val second = StubService("second")
        val manager = ToolManager().also { JeiTools.register(it) }
        try {
            JeiQueryGateway.install(first)
            JeiQueryGateway.install(second) // a reload installs the new runtime...
            JeiQueryGateway.uninstall(first) // ...while the old runtime's shutdown arrives late

            val search = replyOf(manager, SEARCH, buildJsonObject { put("filter", "iron") })
            val recipes = replyOf(manager, RECIPES, buildJsonObject { put("ingredient_id", "minecraft:stone") })
            assertEquals("second", search.getValue("tag").jsonPrimitive.content)
            assertEquals("second", recipes.getValue("tag").jsonPrimitive.content)

            JeiQueryGateway.uninstall(second)
            assertEquals("jei_not_ready", errorOf(manager, SEARCH, buildJsonObject { put("filter", "iron") }))
        } finally {
            JeiQueryGateway.uninstall(first)
            JeiQueryGateway.uninstall(second)
        }
    }

    // -------------------------------------------------------------------- summary

    @Test
    fun `jei pages summarize counts and paging without leaking the payload`() {
        val ingredients = buildJsonObject {
            put("ok", true)
            put("page", 1)
            put("page_size", 20)
            put("returned", 20)
            put("has_more", true)
            putJsonArray("ingredients") { add(buildJsonObject { put("name", SECRET) }) }
        }
        val recipes = buildJsonObject {
            put("ok", true)
            put("page", 0)
            put("page_size", 5)
            put("returned", 3)
            put("has_more", false)
            putJsonArray("recipes") { add(buildJsonObject { put("output", SECRET) }) }
        }

        val searchLine = ToolUseSummary.finished(SEARCH, ingredients.toString(), probe)
        val recipeLine = ToolUseSummary.finished(RECIPES, recipes.toString(), probe)

        assertEquals(listOf("20", "2"), argsOf(searchLine).drop(1))
        assertEquals(listOf("3", "1"), argsOf(recipeLine).drop(1))
        assertNoLeak(searchLine)
        assertNoLeak(recipeLine)
    }

    @Test
    fun `a truncated jei page reads differently from one with more pages`() {
        fun reply(truncated: Boolean) = buildJsonObject {
            put("ok", true)
            put("page", 0)
            put("returned", 8)
            put("has_more", true)
            put("truncated", truncated)
        }

        val truncated = ToolUseSummary.finished(SEARCH, reply(true).toString(), probe)
        val more = ToolUseSummary.finished(SEARCH, reply(false).toString(), probe)

        assertTrue(truncated != more, "a truncated page must not appear to be complete")
        assertNoLeak(truncated)
    }

    @Test
    fun `jei failures name the cause without copying the backend message`() {
        val reasons = listOf("jei_not_ready", "unknown_key", "unknown_ingredient", "unknown_category")
            .map { code ->
                val line = ToolUseSummary.finished(
                    SEARCH,
                    buildJsonObject {
                        put("ok", false)
                        put("error", code)
                        put("message", "$SECRET message for $code")
                    }.toString(),
                    probe,
                )
                assertNoLeak(line)
                argsOf(line)[1]
            }

        assertEquals(reasons.size, reasons.toSet().size, "every error code must read differently: $reasons")
    }

    @Test
    fun `restored history localizes a stored jei call without raw json`() {
        val messages = listOf<ChatMessage>(
            ChatMessage.User("what makes stone?"),
            ChatMessage.Assistant(
                null,
                listOf(
                    ToolCall(
                        "c1",
                        RECIPES,
                        JsonObject(mapOf("ingredient_id" to JsonPrimitive("$SECRET:stone"))),
                    ),
                ),
            ),
            ChatMessage.Tool(
                "c1",
                buildJsonObject {
                    put("ok", true)
                    put("page", 0)
                    put("returned", 2)
                    put("has_more", false)
                    putJsonArray("recipes") { add(buildJsonObject { put("output", SECRET) }) }
                }.toString(),
            ),
        )

        val entries = ToolUseSummary.history(messages, probe)

        assertEquals(2, entries.size)
        assertEquals(listOf("2", "1"), argsOf(entries[1].text).drop(1))
        for (entry in entries) assertNoLeak(entry.text)
    }
}

/** A stand-in for `Component.translatable(key, *args).string` that keeps keys and arguments visible. */
private val probe: (String, Array<out Any>) -> String = { key, args ->
    if (args.isEmpty()) key else key + "(" + args.joinToString("|") + ")"
}

private fun argsOf(line: String): List<String> {
    val open = line.indexOf('(')
    if (open < 0) return emptyList()
    return line.substring(open + 1, line.length - 1).split("|")
}

/** Anything that must never reach the chat: raw JSON and echoed model input. */
private const val SECRET = "SECRET_MARKER"

private fun assertNoLeak(line: String) {
    for (leak in listOf(SECRET, "{", "}", "\"")) {
        assertTrue(!line.contains(leak), "summary leaked '$leak': $line")
    }
}
