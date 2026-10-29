package com.rcdiamond.aifactory.addon.integrated_dynamics.tool

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun accepted(arguments: JsonObject): IntegratedDynamicsToolArguments.Listing =
    IntegratedDynamicsToolArguments.listParts(arguments)

private fun rejected(arguments: JsonObject) {
    assertFailsWith<IllegalArgumentException> { accepted(arguments) }
}

class IntegratedDynamicsToolArgumentsTest {


    // ------------------------------------------------------------------ filter

    @Test
    fun `filter is trimmed but keeps its case and may be empty`() {
        assertEquals("block reader", accepted(buildJsonObject { put("filter", "  block reader ") }).filter)
        assertEquals("", accepted(buildJsonObject { put("filter", "   ") }).filter)
        assertEquals("", accepted(buildJsonObject { put("filter", "") }).filter)
    }

    @Test
    fun `filter length boundary is inclusive and non-strings are rejected`() {
        val maximum = "f".repeat(IntegratedDynamicsToolLimits.MAX_FILTER_LENGTH)
        assertEquals(maximum, accepted(buildJsonObject { put("filter", maximum) }).filter)

        rejected(buildJsonObject { put("filter", maximum + "f") })
        rejected(buildJsonObject { put("filter", 7) })
        rejected(buildJsonObject { put("filter", JsonNull) })
        rejected(buildJsonObject { put("filter", true) })
    }

    // ------------------------------------------------------------------ paging

    @Test
    fun `page and page size boundaries are inclusive`() {
        val lowest = accepted(
            buildJsonObject {
                put(IntegratedDynamicsToolArguments.PAGE, 0)
                put(IntegratedDynamicsToolArguments.PAGE_SIZE, 1)
            },
        )
        assertEquals(0, lowest.page)
        assertEquals(1, lowest.pageSize)

        val highest = accepted(
            buildJsonObject {
                put(IntegratedDynamicsToolArguments.PAGE, IntegratedDynamicsToolLimits.MAX_PAGE)
                put(IntegratedDynamicsToolArguments.PAGE_SIZE, IntegratedDynamicsToolLimits.MAX_PAGE_SIZE)
            },
        )
        assertEquals(IntegratedDynamicsToolLimits.MAX_PAGE, highest.page)
        assertEquals(IntegratedDynamicsToolLimits.MAX_PAGE_SIZE, highest.pageSize)

        rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE, IntegratedDynamicsToolLimits.MAX_PAGE + 1) })
        rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE, -1) })
        rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE_SIZE, IntegratedDynamicsToolLimits.MAX_PAGE_SIZE + 1) })
        rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE_SIZE, 0) })
    }

    @Test
    fun `numbers must actually be integers`() {
        // A fraction, a numeric string, an explicit null and a boolean are all refusals, not paging.
        for (value in listOf(JsonPrimitive(1.5), JsonPrimitive("3"), JsonNull, JsonPrimitive(true))) {
            rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE, value) })
            rejected(buildJsonObject { put(IntegratedDynamicsToolArguments.PAGE_SIZE, value) })
        }
    }

    // ------------------------------------------------------------------ unknown

    @Test
    fun `unknown parameters are refused instead of ignored`() {
        rejected(buildJsonObject { put("nbt", "x") })
        rejected(buildJsonObject { put("offset", 5) })
        rejected(buildJsonObject { put("filter", "x"); put("limit", 5) })
        rejected(buildJsonObject { put("filter", "x"); put("page", 0); put("page_size", 5); put("sort", "name") })
    }

    // ------------------------------------------------------------------ network info

    @Test
    fun `network info takes no parameters at all`() {
        assertFailsWith<IllegalArgumentException> { IntegratedDynamicsToolArguments.networkInfo(buildJsonObject { put("page", 0) }) }
        assertFailsWith<IllegalArgumentException> { IntegratedDynamicsToolArguments.networkInfo(buildJsonObject { put("filter", "x") }) }
    }

    // ------------------------------------------------------------------ response envelope

    @Test
    fun `a page reports its total, window and derived has_more`() {
        val items = List(20) { buildJsonObject { put("id", it) } }
        val first = IntegratedDynamicsToolResponse.page(items, total = 25, page = 0, pageSize = 20)

        assertEquals(true, first.getValue("has_more").jsonPrimitive.content.toBoolean())

        val last = IntegratedDynamicsToolResponse.page(items.take(5), total = 25, page = 1, pageSize = 20)
        assertEquals(false, last.getValue("has_more").jsonPrimitive.content.toBoolean(), "the final page must end the read")
    }

    @Test
    fun `an empty result is a completed page`() {
        val empty = IntegratedDynamicsToolResponse.page(emptyList(), total = 0, page = 0, pageSize = 20)
        assertEquals(false, empty.getValue("has_more").jsonPrimitive.content.toBoolean())
        assertEquals(0, empty.getValue("returned").jsonPrimitive.content.toInt())
    }

}
