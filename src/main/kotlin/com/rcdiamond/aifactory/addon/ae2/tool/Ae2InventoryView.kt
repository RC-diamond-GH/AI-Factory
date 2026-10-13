package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.Locale

/**
 * Deterministic filtering, ordering, paging and response shaping for one inventory read.
 *
 * The rows carry an opaque handle so this file never touches AE or Minecraft types: the executor puts
 * the live AE key in [row.handle] and renders only the page it actually returns. That keeps every
 * decision — which rows exist, in which order, on which page — testable, and guarantees the same
 * network contents always produce the same page numbers.
 */
object Ae2InventoryView {
    /** One inventory key before any ordering decision. */
    data class Row<T>(val id: String, val name: String, val count: Long, val handle: T)

    /** One selected page plus the metadata the model needs to ask for the next one. */
    data class Page<T>(
        val rows: List<Row<T>>,
        val page: Int,
        val pageSize: Int,
        val pageCount: Int,
        val totalItems: Int,
        val totalAmount: Long,
    )

    /**
     * Orders by display name (case insensitive, then by id) so the listing is stable between calls,
     * keeps every row matching [filter] as a case insensitive substring of its name or id, and returns
     * the requested page. A page beyond the end is clamped to the last page so a model that overshoots
     * still sees real data.
     */
    fun <T> select(
        rows: List<Row<T>>, filter: String, page: Int, pageSize: Int,
        identity: (T) -> String = { "" },
    ): Page<T> {
        require(pageSize >= 1) { "pageSize must be at least 1" }
        require(page >= 0) { "page must not be negative" }

        val matching = if (filter.isEmpty()) {
            rows
        } else {
            val needle = filter.lowercase(Locale.ROOT)
            rows.filter {
                it.name.lowercase(Locale.ROOT).contains(needle) || it.id.lowercase(Locale.ROOT).contains(needle)
            }
        }

        val ordered = matching.sortedWith(
            compareBy<Row<T>>({ it.name.lowercase(Locale.ROOT) }, { it.id }, { it.name })
                .thenComparator { left, right -> identity(left.handle).compareTo(identity(right.handle)) },
        )

        val totalItems = ordered.size
        val pageCount = if (totalItems == 0) 0 else ((totalItems - 1) / pageSize) + 1
        val effectivePage = if (pageCount == 0) 0 else page.coerceAtMost(pageCount - 1)
        val from = (effectivePage.toLong() * pageSize.toLong()).coerceAtMost(totalItems.toLong()).toInt()
        val to = (from.toLong() + pageSize.toLong()).coerceAtMost(totalItems.toLong()).toInt()

        var totalAmount = 0L
        for (row in ordered) totalAmount = saturatingAdd(totalAmount, row.count)

        return Page(
            rows = ordered.subList(from, to).toList(),
            page = effectivePage,
            pageSize = pageSize,
            pageCount = pageCount,
            totalItems = totalItems,
            totalAmount = totalAmount,
        )
    }

    /**
     * Response payload for [page]. [item] renders one selected row, which is where the executor adds
     * the full key fingerprint and the craftable indicator: only rows on this page pay that cost.
     */
    fun <T> toJson(page: Page<T>, filter: String, item: (T) -> JsonObject): JsonObject = buildJsonObject {
        put("ok", true)
        put("filter", filter)
        put("page", page.page)
        put("page_size", page.pageSize)
        put("page_count", page.pageCount)
        put("total_items", page.totalItems)
        put("total_amount", page.totalAmount)
        put("has_more", page.page + 1 < page.pageCount)
        putJsonArray("items") {
            for (row in page.rows) add(item(row.handle))
        }
    }

    private fun saturatingAdd(left: Long, right: Long): Long {
        val sum = left + right
        if (right > 0 && sum < left) return Long.MAX_VALUE
        if (right < 0 && sum > left) return Long.MIN_VALUE
        return sum
    }
}
