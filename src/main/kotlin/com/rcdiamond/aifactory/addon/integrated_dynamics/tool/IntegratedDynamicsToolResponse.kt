package com.rcdiamond.aifactory.addon.integrated_dynamics.tool

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Pure shaping of the `{ok:true,...}` replies the client tools hand back to the model.
 *
 * The server side owns the real Integrated Dynamics data and the ids inside it, then hands that data
 * to these builders, so the wire shape stays identical for both listing operations and is testable
 * without booting the game. Nothing here references Integrated Dynamics or Minecraft.
 */
object IntegratedDynamicsToolResponse {
    /**
     * One bounded page of a listing.
     *
     * [total] is the number of matching entries before pagination; [page] and [pageSize] are the
     * resolved request that produced `items`. [hasMore] is derived, never caller supplied, so the
     * model can keep reading while it is true.
     */
    fun page(items: List<JsonObject>, total: Int, page: Int, pageSize: Int): JsonObject {
        require(total >= 0) { "total must not be negative" }
        require(page >= 0) { "page must not be negative" }
        require(pageSize >= 1) { "page size must be positive" }
        val consumed = page.toLong() * pageSize + items.size
        return buildJsonObject {
            put("ok", true)
            put("total", total)
            put("page", page)
            put("page_size", pageSize)
            put("returned", items.size)
            put("has_more", consumed < total)
            putJsonArray("items") { items.forEach { add(it) } }
        }
    }

    /** The `ok`-prefixed network summary; [fields] are the already shaped summary entries. */
    fun summary(fields: JsonObject): JsonObject = buildJsonObject {
        put("ok", true)
        fields.forEach { (key, value) -> put(key, value) }
    }
}
