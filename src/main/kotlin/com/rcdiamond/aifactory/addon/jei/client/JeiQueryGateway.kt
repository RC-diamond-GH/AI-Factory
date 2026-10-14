package com.rcdiamond.aifactory.addon.jei.client

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pure query contracts: no JEI or Minecraft types may cross this boundary. */
data class JeiIngredientQuery(val filter: String, val page: Int = 0, val pageSize: Int = 20)

data class JeiRecipeQuery(
    val ingredientKey: String? = null,
    val ingredientType: String = "item",
    val ingredientId: String? = null,
    val direction: String = "recipes",
    val categoryId: String? = null,
    val page: Int = 0,
    val pageSize: Int = 5,
)

interface JeiQueryService {
    fun findIngredients(query: JeiIngredientQuery): JsonObject
    fun queryRecipes(query: JeiRecipeQuery): JsonObject
}

/** Client-thread gateway. JEI supplies the implementation only while its runtime is alive. */
object JeiQueryGateway : JeiQueryService {
    private var service: JeiQueryService? = null

    fun install(service: JeiQueryService) {
        this.service = service
    }

    fun uninstall(service: JeiQueryService) {
        if (this.service === service) this.service = null
    }

    override fun findIngredients(query: JeiIngredientQuery): JsonObject =
        service?.findIngredients(query) ?: unavailable()

    override fun queryRecipes(query: JeiRecipeQuery): JsonObject =
        service?.queryRecipes(query) ?: unavailable()

    private fun unavailable(): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", "jei_not_ready")
        put("message", "JEI is not ready, or its runtime is being reloaded. No query was performed.")
    }
}
