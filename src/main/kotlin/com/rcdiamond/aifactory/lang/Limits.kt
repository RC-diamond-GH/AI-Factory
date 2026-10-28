package com.rcdiamond.aifactory.lang

/**
 * Immutable resource limits for lexing, parsing, checking and graph lowering.
 *
 * Every limit is enforced by returning a [DiagnosticCode.E_LIMIT] diagnostic instead of allowing a
 * stack overflow, unbounded recursion or unbounded graph construction. The defaults are safe for
 * interactive editing; tools may publish stricter values with [com.rcdiamond.aifactory.lang.LanguageLimits].
 *
 * Values must be positive. A caller that wants effectively unbounded behavior should choose a large
 * finite number rather than rely on disabling checks.
 */
data class LanguageLimits(
    /** UTF-8 byte length of the source text. */
    val maxSourceBytes: Int = 256 * 1024,
    /** UTF-16 code units of the source text. */
    val maxSourceChars: Int = 256 * 1024,
    /** Number of lexical tokens, excluding EOF. */
    val maxTokens: Int = 100_000,
    /** Number of top-level declarations. */
    val maxDeclarations: Int = 4_096,
    /** Parsing/AST depth and checking/lowering traversal depth, including expansion across let references. */
    val maxNestingDepth: Int = 128,
    /** Structural depth of a type, including environment schemes that reach the checker. */
    val maxTypeDepth: Int = 32,
    /** Elements in one list literal. */
    val maxListLength: Int = 4_096,
    /** UTF-16 units in one decoded string literal. */
    val maxStringLength: Int = 64 * 1024,
    /** Number of nodes emitted by graph lowering. */
    val maxNodes: Int = 8_192,
    /** Depth of let-to-let dependency chains and cycle searches. */
    val maxDependencyDepth: Int = 512,
    /** Total diagnostics retained; later findings are dropped. */
    val maxDiagnostics: Int = 128,
) {
    init {
        require(maxSourceBytes > 0)
        require(maxSourceChars > 0)
        require(maxTokens > 0)
        require(maxDeclarations > 0)
        require(maxNestingDepth > 0)
        require(maxTypeDepth > 0)
        require(maxListLength > 0)
        require(maxStringLength > 0)
        require(maxNodes > 0)
        require(maxDependencyDepth > 0)
        require(maxDiagnostics > 0)
    }

    companion object {
        val DEFAULT: LanguageLimits = LanguageLimits()
    }
}

/**
 * Limits for a pure backend run. Evaluation depth is separate from [LanguageLimits] because a graph
 * can be legally deep while still needing a bounded interpreter stack.
 */
data class EvaluationLimits(
    /** Node evaluations, primitive invocations and closure applications before aborting with E_LIMIT. */
    val maxSteps: Int = 1_000_000,
    /** Recursive evaluation depth. */
    val maxDepth: Int = 512,
    /** Elements materialized by one list-producing operation. */
    val maxListElements: Int = 100_000,
    /** UTF-16 code units (Kotlin [String.length]) materialized by one string-producing operation. */
    val maxStringLength: Int = 1_048_576,
) {
    init {
        require(maxSteps > 0)
        require(maxDepth > 0)
        require(maxListElements > 0)
        require(maxStringLength > 0)
    }

    companion object {
        val DEFAULT: EvaluationLimits = EvaluationLimits()
    }
}
