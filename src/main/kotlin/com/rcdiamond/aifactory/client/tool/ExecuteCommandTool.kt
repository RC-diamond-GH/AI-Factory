package com.rcdiamond.aifactory.client.tool

import com.rcdiamond.aifactory.client.llm.ExecuteCommandGateway
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.server.CommandLimits
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Advertises and validates one `execute_command` call.
 *
 * The tool owns only the schema and the argument policy: the command text is normalized by
 * [CommandLimits] and handed to [gateway]. The command runs as the asking player with their current
 * permissions, position, rotation and dimension; those checks stay authoritative on the server.
 */
class ExecuteCommandTool(private val gateway: ExecuteCommandGateway) : ClientTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = NAME,
        description = "Runs one Minecraft command as the player asking for it, with exactly their current " +
            "permissions, position, rotation and dimension. Commands from vanilla and from modded servers " +
            "are both accepted. The leading slash is optional. The command may refer to the executing player " +
            "with @s and use relative coordinates such as ~ ~ ~. Only use commands to perform actions " +
            "the player explicitly requested. The reported result code is informational: a code of 0 does not by " +
            "itself mean the call failed, success is reported separately. A command that changes the world " +
            "cannot be promised to be reversible, so never claim it will be undone.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject(COMMAND) {
                    put("type", "string")
                    put("minLength", 1)
                    put("maxLength", CommandLimits.MAX_COMMAND_LENGTH)
                    put(
                        "description",
                        "Command to run, with or without a leading slash, " +
                            "for example '/give @s minecraft:diamond' or 'time set day'.",
                    )
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive(COMMAND))
            }
            put("additionalProperties", false)
        },
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val parameters = arguments as? JsonObject ?: throw ToolArgumentException("$NAME arguments must be a JSON object")
        val unknown = parameters.keys - PARAMETERS
        if (unknown.isNotEmpty()) {
            throw ToolArgumentException("$NAME has no parameters named ${unknown.sorted().joinToString(", ")}")
        }
        val missing = PARAMETERS - parameters.keys
        if (missing.isNotEmpty()) {
            throw ToolArgumentException("$NAME requires ${missing.sorted().joinToString(", ")}")
        }
        val command = CommandLimits.normalize(parameters.stringParameter(COMMAND))
            ?: throw ToolArgumentException(
                "$NAME command must be non-blank, at most ${CommandLimits.MAX_COMMAND_LENGTH} characters, " +
                    "and free of line breaks and NUL",
            )
        val result = gateway.executeCommand(command)
        return buildJsonObject {
            put(COMMAND, command)
            put("success", result.success)
            put("result_code", result.resultCode)
            put("message", result.message)
            put("output_truncated", result.outputTruncated)
        }
    }

    /** Reads a required string parameter, rejecting numbers, booleans and null. */
    private fun JsonObject.stringParameter(name: String): String {
        val value = this[name]
        if (value !is JsonPrimitive || !value.isString) {
            throw ToolArgumentException("$NAME parameter '$name' must be a string")
        }
        return value.content
    }

    companion object {
        const val NAME = "execute_command"

        private const val COMMAND = "command"

        private val PARAMETERS = setOf(COMMAND)
    }
}
