package com.rcdiamond.aifactory.server

import com.mojang.brigadier.ResultConsumer
import com.rcdiamond.aifactory.client.llm.ExecuteCommandRequest
import com.rcdiamond.aifactory.client.llm.ExecuteCommandResult
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * Server-authoritative implementation of the `execute_command` game action.
 *
 * The command runs as the asking player and nothing else: it uses that player's own [CommandSourceStack],
 * so `@s`, relative coordinates, rotation and dimension all resolve to them, their permissions stay
 * untouched (a command they may not run fails exactly as it would in chat, no level is granted) and Forge's
 * `CommandEvent` still fires because the text goes through
 * [net.minecraft.commands.Commands.performPrefixedCommand]. Only the feedback the player would have seen is
 * captured, capped at [CommandLimits.MAX_OUTPUT_LENGTH], so a command that spams chat can not flood the
 * answer. Nothing is rolled back: a command that changed the world stays changed.
 *
 * Must be called on the server thread with a [ServerPlayer] that belongs to a running server.
 */
internal object ExecuteCommandMechanism {

    /** Answer text used when [CommandLimits.normalize] refuses the raw request. */
    private val REJECTED_MESSAGE: String =
        "Rejected the command: it must be non-blank, no longer than ${CommandLimits.MAX_COMMAND_LENGTH} " +
            "characters and free of line breaks or NUL"

    fun execute(player: ServerPlayer, request: ExecuteCommandRequest): ExecuteCommandResult {
        val requestId = request.requestId
        // Re-normalize here as well: a modified client can bypass the tool and lie about the command text.
        val command = CommandLimits.normalize(request.command)
            ?: return ExecuteCommandResult(requestId, false, 0, REJECTED_MESSAGE, false)

        val feedback = CommandFeedback(CommandLimits.MAX_OUTPUT_LENGTH)
        val base = player.createCommandSourceStack()
        // Delegating shouldInformAdmins keeps the player's own admin-audit policy for feedback.
        val capture = object : CommandSource {
            override fun sendSystemMessage(message: Component) {
                feedback.append(message.string)
            }

            override fun acceptsSuccess(): Boolean = true

            override fun acceptsFailure(): Boolean = true

            override fun shouldInformAdmins(): Boolean = base.source.shouldInformAdmins()
        }
        val stack = base
            .withSource(capture)
            .withCallback(ResultConsumer<CommandSourceStack> { _, success, _ -> feedback.onCommandComplete(success) })

        // performPrefixedCommand keeps Forge's CommandEvent and vanilla's own error handling and returns
        // the command's result code, which is informational only: 0 does not by itself mean failure.
        val resultCode = base.server.commands.performPrefixedCommand(stack, command)

        val output = feedback.output
        val message = if (output.isNotEmpty()) {
            output
        } else if (feedback.succeeded) {
            "Command completed (result code $resultCode); it produced no feedback"
        } else if (feedback.called) {
            "Command failed (result code $resultCode); it produced no feedback"
        } else {
            "Command did not report completion (result code $resultCode); it may have been cancelled"
        }
        return ExecuteCommandResult(requestId, feedback.succeeded, resultCode, message, feedback.truncated)
    }
}

/**
 * Accumulates the bounded feedback of one command execution and tracks what Brigadier reported.
 *
 * Free of Minecraft types so the outcome rules can be unit-tested: the text is capped at [limit] instead of
 * collecting an unbounded list, [succeeded] is only true when a command node actually completed (a result
 * value of 0 still counts) and silence is never success.
 */
internal class CommandFeedback(private val limit: Int) {

    private val text = StringBuilder()
    /** True once at least one executed command node reported a result, success or failure. */
    var called: Boolean = false
        private set

    /** True when at least one command node completed without throwing, even when it returned 0. */
    var succeeded: Boolean = false
        private set

    /** True when feedback was dropped because it would have exceeded [limit]. */
    var truncated: Boolean = false
        private set

    /** The collected feedback, never longer than [limit]. */
    val output: String get() = text.toString()

    /** Records one Brigadier completion callback for a command node. */
    fun onCommandComplete(success: Boolean) {
        called = true
        if (success) succeeded = true
    }

    /** Appends one feedback line, dropping whatever would exceed [limit]. */
    fun append(message: String) {
        if (message.isEmpty() || truncated) return
        val separator = if (text.isEmpty()) 0 else 1
        val room = limit - text.length - separator
        if (room <= 0) {
            truncated = true
            return
        }
        if (separator == 1) text.append('\n')
        if (message.length <= room) {
            text.append(message)
        } else {
            text.append(message, 0, room)
            truncated = true
        }
    }
}
