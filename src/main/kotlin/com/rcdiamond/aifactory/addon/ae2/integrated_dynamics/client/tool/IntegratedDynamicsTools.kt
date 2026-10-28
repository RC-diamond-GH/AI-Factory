package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolArguments
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.client.tool.ToolArgumentException
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Registers ID inspection plus client-side DSL preview and explicit native-card creation.
 *
 * These tools depend only on pure Kotlin/JSON contracts. The gateway binds game requests to the
 * leased terminal; compilation runs off the game thread and never evaluates live network values.
 */
object IntegratedDynamicsTools {
    /** Every operation the registered tools may send to the gateway; anything else is a bug. */
    val OPERATIONS: Set<String> = IntegratedDynamicsToolLimits.OPERATIONS

    fun register(manager: ToolManager, gateway: IntegratedDynamicsToolGateway) {
        manager.register(IdNetworkInfoTool(gateway))
        manager.register(IdListPartsTool(gateway))
        manager.register(IdListVariablesTool(gateway))
        manager.register(IdDslTool(gateway, createCards = false))
        manager.register(IdDslTool(gateway, createCards = true))
    }

    /** Reads the call's arguments as an object. */
    internal fun parameters(name: String, arguments: JsonElement): JsonObject =
        arguments as? JsonObject ?: throw ToolArgumentException("$name arguments must be a JSON object")
}

/**
 * The shared `{filter,page,page_size}` schema for both listing tools.
 *
 * Only the [filterDescription] differs between parts and variables; the bounds come from the limits
 * the server also enforces, so the advertised schema can never drift from what is accepted.
 */
internal fun listingSchema(filterDescription: String): JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject(IntegratedDynamicsToolArguments.FILTER) {
            put("type", "string")
            put("maxLength", IntegratedDynamicsToolLimits.MAX_FILTER_LENGTH)
            put("description", filterDescription)
        }
        putJsonObject(IntegratedDynamicsToolArguments.PAGE) {
            put("type", "integer")
            put("minimum", 0)
            put("maximum", IntegratedDynamicsToolLimits.MAX_PAGE)
            put("description", "Zero based page number. Defaults to 0, the first page.")
        }
        putJsonObject(IntegratedDynamicsToolArguments.PAGE_SIZE) {
            put("type", "integer")
            put("minimum", 1)
            put("maximum", IntegratedDynamicsToolLimits.MAX_PAGE_SIZE)
            put(
                "description",
                "Entries per page, 1 to ${IntegratedDynamicsToolLimits.MAX_PAGE_SIZE}. " +
                    "Defaults to ${IntegratedDynamicsToolLimits.DEFAULT_PAGE_SIZE}.",
            )
        }
    }
    put("additionalProperties", false)
}

/** Turns a shared validation failure into the model-correctable error the tool manager reports. */
internal inline fun <T> validated(block: () -> T): T = try {
    block()
} catch (invalid: IllegalArgumentException) {
    throw ToolArgumentException(invalid.message ?: "invalid arguments")
}
