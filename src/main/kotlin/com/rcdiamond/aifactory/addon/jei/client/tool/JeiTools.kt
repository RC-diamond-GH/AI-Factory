package com.rcdiamond.aifactory.addon.jei.client.tool

import com.rcdiamond.aifactory.addon.jei.client.JeiQueryGateway
import com.rcdiamond.aifactory.addon.jei.client.JeiQueryService
import com.rcdiamond.aifactory.client.tool.ToolArgumentException
import com.rcdiamond.aifactory.client.tool.ToolManager
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Registers the two read-only JEI query tools.
 *
 * The tools hold only schemas and argument policy; every request is handed to [gateway], which the
 * client composition root wires to the live JEI runtime. Nothing here references the JEI API or
 * Minecraft, so only the caller decides whether JEI is loaded, and the gateway answers with a
 * `jei_not_ready` observation while JEI is absent or reloading instead of throwing.
 *
 * The default [gateway] is the shared [JeiQueryGateway], which the optional JEI integration installs
 * into while its runtime is alive; because the tools read it per call, a reload that replaces the
 * backend is picked up without re-registering anything.
 */
object JeiTools {
    fun register(manager: ToolManager, gateway: JeiQueryService = JeiQueryGateway) {
        manager.register(JeiFindIngredientsTool(gateway))
        manager.register(JeiQueryRecipesTool(gateway))
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
