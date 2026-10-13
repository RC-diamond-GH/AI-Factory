package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.ToolCall
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Ordered registry of client tools and the single place a model tool call is turned into a reply.
 *
 * Registration order is the order tools are advertised to the model. A call never throws because of
 * what the model sent: an unknown tool name or arguments that break a tool's schema become a
 * structured observation the model can react to. Everything else — implementation bugs and
 * coroutine cancellation — propagates unchanged.
 */
class ToolManager {
    private val tools = LinkedHashMap<String, ClientTool>()
    private var advertised: List<ToolDefinition> = emptyList()

    /** Registers [tool] under its definition name. Duplicate names are rejected. */
    fun register(tool: ClientTool) {
        val name = tool.definition.name
        require(name.isNotBlank()) { "tool name must not be blank" }
        require(tools.putIfAbsent(name, tool) == null) { "tool '$name' is already registered" }
        advertised = tools.values.map { it.definition }
    }

    /** Advertised definitions in stable registration order. */
    val definitions: List<ToolDefinition> get() = advertised

    /**
     * Runs [call] and returns the reply for the assistant message that requested it. The reply keeps
     * the call id so the conversation stays correlatable.
     */
    suspend fun execute(call: ToolCall): ChatMessage.Tool {
        val tool = tools[call.name]
        val arguments = call.arguments
        val content = when {
            tool == null -> toolFailureObservation(call.name, "unknown_tool", "No tool named '${call.name}' is registered")
            arguments !is JsonObject -> toolFailureObservation(call.name, "invalid_arguments", "Arguments must be a JSON object")
            else -> try {
                tool.execute(arguments).toString()
            } catch (invalid: ToolArgumentException) {
                toolFailureObservation(call.name, "invalid_arguments", invalid.message ?: "Invalid arguments")
            }
        }
        return ChatMessage.Tool(call.id, content)
    }
}

/** Structured observation shown to the model when a call produced no tool result. */
internal fun toolFailureObservation(tool: String, error: String, message: String): String = buildJsonObject {
    put("tool", tool)
    put("error", error)
    put("message", message)
}.toString()
