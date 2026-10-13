package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.ToolDefinition
import kotlinx.serialization.json.JsonElement

/**
 * A capability the client advertises to the model.
 *
 * A tool owns only its provider-facing schema and the policy for its arguments; anything that
 * changes game state must be delegated to a gateway (see
 * [com.rcdiamond.aifactory.client.llm.GiveItemGateway]) so client tools never touch game classes.
 */
interface ClientTool {
    /** Advertised schema. Its name is the registration key inside a [ToolManager]. */
    val definition: ToolDefinition

    /** Runs one call whose arguments are already parsed, and returns the observation for the model. */
    suspend fun execute(arguments: JsonElement): JsonElement
}

/**
 * Expected, model-correctable tool failure: the arguments violate the tool's schema.
 *
 * Tools throw this instead of arbitrary exceptions so [ToolManager] can report it to the model,
 * while implementation bugs and coroutine cancellation keep propagating.
 */
class ToolArgumentException(message: String) : Exception(message)
