package com.rcdiamond.aifactory.addon.ae2.client.tool

import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import com.rcdiamond.aifactory.client.tool.ToolArgumentException
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Registers the five ME terminal tools.
 *
 * The tools hold only schemas and argument policy; every request is handed to [gateway], which the
 * client composition root wires to the locked terminal session. Nothing here references the Applied
 * Energistics API or Minecraft, so only the caller decides whether AE2 is present.
 *
 * [gateway] receives an operation from [Ae2ToolLimits.OPERATIONS] and the already validated arguments,
 * and must return the server's reply object (`{ok:true,...}` or `{ok:false,error:...}`). A missing or
 * closed terminal session must come back as an `ok:false` object, not as a thrown exception: a throw
 * aborts the agent's whole turn instead of telling the model what happened.
 */
object Ae2Tools {
    fun register(manager: ToolManager, gateway: suspend (String, JsonObject) -> JsonObject) {
        manager.register(Ae2ReadInventoryTool(gateway))
        manager.register(Ae2ItemDetailsTool(gateway))
        manager.register(Ae2CraftingPlanTool(gateway))
        manager.register(Ae2RequestCraftingTool(gateway))
        manager.register(Ae2ManagePatternsTool(gateway))
    }

    /** Reads the call's arguments as an object. */
    internal fun parameters(name: String, arguments: JsonElement): JsonObject =
        arguments as? JsonObject ?: throw ToolArgumentException("$name arguments must be a JSON object")
}

/** Turns a shared validation failure into the model-correctable error the tool manager reports. */
internal inline fun <T> validated(block: () -> T): T = try {
    block()
} catch (invalid: IllegalArgumentException) {
    throw ToolArgumentException(invalid.message ?: "invalid arguments")
}
