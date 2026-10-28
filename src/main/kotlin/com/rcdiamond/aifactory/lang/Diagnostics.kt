package com.rcdiamond.aifactory.lang

enum class Severity { WARNING, ERROR }

/**
 * Stable diagnostic codes for the shared language. Consumers must switch on [DiagnosticCode], not on
 * message text. Codes follow the Integrated Dynamics DSL contract; a few additional codes cover
 * concerns that only exist in the reusable core (unknown type names and pure-evaluation failures).
 */
enum class DiagnosticCode {
    E_SYNTAX,
    E_UNKNOWN_NAME,
    E_DUPLICATE_NAME,
    E_CYCLE,
    E_INPUT_BINDING,
    E_TYPE,
    E_UNRESOLVED_TYPE,
    E_NOT_CALLABLE,
    E_FN_SIGNATURE,
    E_EMPTY_LIST_TYPE,
    E_LITERAL_RANGE,
    E_LITERAL_RESOURCE,
    E_LITERAL_NBT,
    E_NATIVE_CONTRACT,
    E_INFINITE_CONSUMER,
    E_UNSUPPORTED_SYNTAX,
    E_LIMIT,
    E_TARGET_CONTRACT,
    E_UNKNOWN_TYPE,
    E_EVALUATION,
    W_UNUSED_BINDING,
}

data class RelatedDiagnostic(val message: String, val span: SourceSpan)

/**
 * One compile/evaluation finding. Source ranges are always UTF-16 offsets into the compiled
 * [SourceText]; [SourceText.lineText] and [SourceText.positionAt] turn them into one-based lines and
 * columns for people.
 */
data class Diagnostic(
    val code: DiagnosticCode,
    val severity: Severity,
    val message: String,
    val span: SourceSpan,
    val related: List<RelatedDiagnostic> = emptyList(),
) {
    init {
        require(message.isNotBlank()) { "diagnostic message must not be blank" }
    }

    val isError: Boolean get() = severity == Severity.ERROR

    fun render(source: SourceText): String {
        val builder = StringBuilder()
        builder.append(if (isError) "error" else "warning")
        builder.append('[').append(code.name).append(']')
        builder.append(' ').append(source.describe(span))
        builder.append(": ").append(message)
        for (item in related) {
            builder.append('\n').append("  related ").append(source.describe(item.span))
                .append(": ").append(item.message)
        }
        return builder.toString()
    }

    companion object {
        fun error(code: DiagnosticCode, span: SourceSpan, message: String): Diagnostic =
            Diagnostic(code, Severity.ERROR, message, span)

        fun warning(code: DiagnosticCode, span: SourceSpan, message: String): Diagnostic =
            Diagnostic(code, Severity.WARNING, message, span)
    }
}

internal fun List<Diagnostic>.hasErrors(): Boolean = any { it.isError }
