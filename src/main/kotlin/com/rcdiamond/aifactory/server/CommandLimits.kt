package com.rcdiamond.aifactory.server

/**
 * Bounds and text normalization for one execute-command request, enforced on the server.
 *
 * Deliberately free of Minecraft types, so the client tool can share the very same rules and the policy is
 * unit-testable without a running game. The limits only bound the raw text: a normalized command is never
 * split or reinterpreted (semicolons, quoting, extra spaces and NBT stay verbatim), and the command
 * dispatcher remains the only thing that decides whether a command is valid.
 */
object CommandLimits {
    /** Longest accepted command text; only the raw request length is bounded. */
    const val MAX_COMMAND_LENGTH: Int = 32767

    /** Longest feedback text returned to the caller; longer feedback is truncated. */
    const val MAX_OUTPUT_LENGTH: Int = 8192

    /**
     * Returns the text to hand to the dispatcher, or `null` when the raw [command] must be rejected.
     *
     * Rejects oversized, blank or multiline text, NUL and repeated leading slashes. Otherwise the outer
     * whitespace is trimmed and one optional leading `/` is removed. Rejecting repeated slashes keeps
     * normalization idempotent across the tool and server; the remaining command text is kept
     * verbatim, so internal spacing, quoting, semicolons and NBT are forwarded untouched.
     */
    fun normalize(command: String): String? {
        if (command.length > MAX_COMMAND_LENGTH) return null
        if (command.any { it == '\r' || it == '\n' || it == '\u0000' }) return null
        var normalized = command.trim()
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trimStart()
        }
        if (normalized.startsWith("/")) return null
        return normalized.ifEmpty { null }
    }
}
