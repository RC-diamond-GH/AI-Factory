package com.rcdiamond.aifactory.addon.ae2

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun row(id: String, name: String, count: Long, handle: String = id) =
    Ae2InventoryView.Row(id = id, name = name, count = count, handle = handle)

class Ae2InventoryViewTest {
    @Test
    fun `NBT variants with identical names keep stable pagination`() {
        val a = row("minecraft:stone", "Stone", 1, "variant-a")
        val b = row("minecraft:stone", "Stone", 1, "variant-b")
        val first = Ae2InventoryView.select(listOf(b, a), "", 0, 1) { it }
        val reordered = Ae2InventoryView.select(listOf(a, b), "", 0, 1) { it }
        val next = Ae2InventoryView.select(listOf(b, a), "", 1, 1) { it }
        assertEquals("variant-a", first.rows.single().handle)
        assertEquals(first.rows, reordered.rows)
        assertEquals("variant-b", next.rows.single().handle)
    }

    @Test
    fun `rows are ordered by name then id so a listing is reproducible`() {
        val page = Ae2InventoryView.select(
            listOf(
                row("minecraft:stone", "Stone", 10),
                row("minecraft:apple", "Apple", 1),
                row("minecraft:apple_golden", "Apple", 2),
            ),
            filter = "",
            page = 0,
            pageSize = 20,
        )
        assertEquals(listOf("minecraft:apple", "minecraft:apple_golden", "minecraft:stone"), page.rows.map { it.id })
        assertEquals(3, page.totalItems)
        assertEquals(13L, page.totalAmount)
        assertEquals(1, page.pageCount)
    }

    @Test
    fun `the filter matches name or id case insensitively`() {
        val rows = listOf(
            row("minecraft:iron_ingot", "Iron Ingot", 64),
            row("thermal:iron_plate", "Iron Plate", 1),
            row("minecraft:stone", "Stone", 5),
        )
        val byId = Ae2InventoryView.select(rows, filter = "thermal", page = 0, pageSize = 20)
        assertEquals(listOf("thermal:iron_plate"), byId.rows.map { it.id })

        val byName = Ae2InventoryView.select(rows, filter = "iron", page = 0, pageSize = 20)
        assertEquals(listOf("minecraft:iron_ingot", "thermal:iron_plate"), byName.rows.map { it.id })
        assertEquals(65L, byName.totalAmount)
    }

    @Test
    fun `paging is stable and clamps a page past the end`() {
        val rows = (0 until 7).map { row("id:$it", "Item %02d".format(it), 1) }

        val first = Ae2InventoryView.select(rows, filter = "", page = 0, pageSize = 3)
        assertEquals(listOf("id:0", "id:1", "id:2"), first.rows.map { it.id })
        assertEquals(3, first.pageCount)
        assertEquals(7, first.totalItems)

        val last = Ae2InventoryView.select(rows, filter = "", page = 2, pageSize = 3)
        assertEquals(listOf("id:6"), last.rows.map { it.id })
        assertEquals(2, last.page)

        val beyond = Ae2InventoryView.select(rows, filter = "", page = 99, pageSize = 3)
        assertEquals(2, beyond.page, "an overshooting page must fall back to the last real page")
        assertEquals(listOf("id:6"), beyond.rows.map { it.id })
    }

    @Test
    fun `an empty network is a valid empty page`() {
        val page = Ae2InventoryView.select(emptyList<Ae2InventoryView.Row<String>>(), filter = "", page = 4, pageSize = 20)
        assertEquals(0, page.pageCount)
        assertEquals(0, page.page)
        assertEquals(0, page.totalItems)
        assertEquals(0L, page.totalAmount)
        assertTrue(page.rows.isEmpty())
    }

    @Test
    fun `the total amount saturates instead of wrapping`() {
        val page = Ae2InventoryView.select(
            listOf(row("a", "A", Long.MAX_VALUE), row("b", "B", 5)),
            filter = "",
            page = 0,
            pageSize = 20,
        )
        assertEquals(Long.MAX_VALUE, page.totalAmount)
    }

    @Test
    fun `only the selected page is rendered`() {
        val rows = (0 until 10).map { row("id:$it", "Item %02d".format(it), it.toLong()) }
        val page = Ae2InventoryView.select(rows, filter = "", page = 0, pageSize = 4)

        val rendered = mutableListOf<String>()
        val json = Ae2InventoryView.toJson(page, filter = "") { handle ->
            rendered += handle
            buildJsonObject { put("key", handle) }
        }

        assertEquals(listOf("id:0", "id:1", "id:2", "id:3"), rendered, "rendering one page must not touch the rest")
        assertEquals(true, json.getValue("ok").jsonPrimitive.boolean)
        assertEquals(0, json.getValue("page").jsonPrimitive.int)
        assertEquals(4, json.getValue("page_size").jsonPrimitive.int)
        assertEquals(3, json.getValue("page_count").jsonPrimitive.int)
        assertEquals(10, json.getValue("total_items").jsonPrimitive.int)
        assertEquals(45L, json.getValue("total_amount").jsonPrimitive.long)
        assertEquals(true, json.getValue("has_more").jsonPrimitive.boolean)
        assertEquals(4, assertIs<JsonArray>(json.getValue("items")).size)
    }

    @Test
    fun `the last page reports no more`() {
        val rows = (0 until 4).map { row("id:$it", "Item %02d".format(it), 1) }
        val page = Ae2InventoryView.select(rows, filter = "", page = 1, pageSize = 3)
        val json = Ae2InventoryView.toJson(page, filter = "item") { buildJsonObject { put("key", it) } }
        assertEquals(1, json.getValue("page").jsonPrimitive.int)
        assertEquals(2, json.getValue("page_count").jsonPrimitive.int)
        assertEquals(false, json.getValue("has_more").jsonPrimitive.boolean)
        assertEquals("item", json.getValue("filter").jsonPrimitive.content)
        assertEquals(1, assertIs<JsonArray>(json.getValue("items")).size)
        assertFalse(page.rows.isEmpty())
    }
}
