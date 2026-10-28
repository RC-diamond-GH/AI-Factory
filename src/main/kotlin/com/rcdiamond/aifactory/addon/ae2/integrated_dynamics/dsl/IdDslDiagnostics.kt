package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import com.rcdiamond.aifactory.lang.DiagnosticCode

/** Stable error/warning codes of the ID DSL contract, shared by the client compiler and the server validator. */
enum class IdDiagnosticCode {
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
    W_UNKNOWN_FINITENESS,
    ;

    val isError: Boolean get() = name.startsWith("E_")

    companion object {
        fun fromLang(code: DiagnosticCode): IdDiagnosticCode = when (code) {
            DiagnosticCode.E_SYNTAX -> E_SYNTAX
            DiagnosticCode.E_UNKNOWN_NAME -> E_UNKNOWN_NAME
            DiagnosticCode.E_DUPLICATE_NAME -> E_DUPLICATE_NAME
            DiagnosticCode.E_CYCLE -> E_CYCLE
            DiagnosticCode.E_INPUT_BINDING -> E_INPUT_BINDING
            DiagnosticCode.E_TYPE -> E_TYPE
            DiagnosticCode.E_UNRESOLVED_TYPE -> E_UNRESOLVED_TYPE
            DiagnosticCode.E_NOT_CALLABLE -> E_NOT_CALLABLE
            DiagnosticCode.E_FN_SIGNATURE -> E_FN_SIGNATURE
            DiagnosticCode.E_EMPTY_LIST_TYPE -> E_EMPTY_LIST_TYPE
            DiagnosticCode.E_LITERAL_RANGE -> E_LITERAL_RANGE
            DiagnosticCode.E_LITERAL_RESOURCE -> E_LITERAL_RESOURCE
            DiagnosticCode.E_LITERAL_NBT -> E_LITERAL_NBT
            DiagnosticCode.E_NATIVE_CONTRACT -> E_NATIVE_CONTRACT
            DiagnosticCode.E_INFINITE_CONSUMER -> E_INFINITE_CONSUMER
            DiagnosticCode.E_UNSUPPORTED_SYNTAX -> E_UNSUPPORTED_SYNTAX
            DiagnosticCode.E_LIMIT -> E_LIMIT
            DiagnosticCode.E_TARGET_CONTRACT -> E_TARGET_CONTRACT
            DiagnosticCode.E_UNKNOWN_TYPE -> E_UNKNOWN_TYPE
            DiagnosticCode.E_EVALUATION -> E_EVALUATION
            DiagnosticCode.W_UNUSED_BINDING -> W_UNUSED_BINDING
        }
    }
}

enum class IdSeverity { ERROR, WARNING }

/** One UTF-16 source range. Offsets match Kotlin strings, not UTF-8 byte offsets. */
data class IdSourceSpan(val start: Int, val end: Int) {
    init {
        require(start >= 0) { "span start must be non-negative" }
        require(end >= start) { "span end must not precede start" }
    }

    companion object {
        val EMPTY = IdSourceSpan(0, 0)
    }
}

data class IdRelatedDiagnostic(val message: String, val span: IdSourceSpan)

/**
 * A diagnostic that survives outside a compiler instance.
 *
 * [line] and [column] are one-based and count UTF-16 units. A wire-only diagnostic has `0` for both,
 * because no source was sent to the server. [node] and [input] let a server report attach to a local
 * plan reference; the client maps it back with [IdSourceMap].
 */
data class IdDiagnostic(
    val code: IdDiagnosticCode,
    val severity: IdSeverity,
    val message: String,
    val span: IdSourceSpan = IdSourceSpan.EMPTY,
    val line: Int = 0,
    val column: Int = 0,
    val hint: String? = null,
    val node: Int? = null,
    val input: String? = null,
    val related: List<IdRelatedDiagnostic> = emptyList(),
) {
    init {
        require(message.isNotBlank()) { "diagnostic message must not be blank" }
        require(line >= 0) { "line must be zero or positive" }
        require(column >= 0) { "column must be zero or positive" }
    }

    val isError: Boolean get() = severity == IdSeverity.ERROR

    fun render(): String {
        val location = if (line > 0) "line $line, column $column" else "plan"
        val builder = StringBuilder()
        builder.append(if (isError) "error" else "warning")
        builder.append('[').append(code.name).append(']').append(' ').append(location)
        builder.append(": ").append(message)
        if (hint != null) builder.append("\n  hint: ").append(hint)
        for (item in related) {
            builder.append("\n  related: ").append(item.message)
        }
        return builder.toString()
    }
}
