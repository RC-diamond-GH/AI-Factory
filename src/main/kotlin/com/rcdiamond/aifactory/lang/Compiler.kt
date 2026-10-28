package com.rcdiamond.aifactory.lang

/**
 * Result of one compile. [module] is non-null only when lexing, parsing, name resolution, checking
 * and lowering all succeeded; a failed compile never returns a deployable partial graph.
 *
 * [program] is retained on semantic failure for editor/tooling display, but a backend must not lower
 * it. [source] is the exact text that all spans refer to.
 */
data class CompilationResult(
    val source: SourceText,
    val diagnostics: List<Diagnostic>,
    val program: Program?,
    val module: TypedModule?,
) {
    val success: Boolean
        get() = module != null && diagnostics.none { it.isError }

    fun renderDiagnostics(): String =
        diagnostics.joinToString("\n") { it.render(source) }
}

/**
 * Pure, thread-safe frontend. One instance may be shared by concurrent compilations; the only state
 * lives in the returned value.
 */
class LangCompiler(
    val environment: LanguageEnvironment,
    val limits: LanguageLimits = LanguageLimits.DEFAULT,
) {
    fun compile(sourceName: String, text: String): CompilationResult {
        if (text.length > limits.maxSourceChars) {
            return sizeFailure(
                SourceText.of(sourceName, ""),
                "source length ${text.length} UTF-16 units exceeds the limit ${limits.maxSourceChars}",
            )
        }
        val byteLength = utf8ByteLength(text, limits.maxSourceBytes)
        if (byteLength > limits.maxSourceBytes) {
            return sizeFailure(
                SourceText.of(sourceName, ""),
                "source exceeds the limit ${limits.maxSourceBytes} UTF-8 bytes",
            )
        }
        return compile(SourceText.of(sourceName, text))
    }

    fun compile(source: SourceText): CompilationResult {
        if (source.length > limits.maxSourceChars) {
            return sizeFailure(
                source,
                "source length ${source.length} UTF-16 units exceeds the limit ${limits.maxSourceChars}",
            )
        }
        if (source.byteLength > limits.maxSourceBytes) {
            return sizeFailure(
                source,
                "source is ${source.byteLength} UTF-8 bytes, exceeding the limit ${limits.maxSourceBytes}",
            )
        }

        val lexical = LanguageLexer(source, limits).lex()
        if (lexical.diagnostics.hasErrors()) {
            return CompilationResult(source, lexical.diagnostics, null, null)
        }

        val parsed = LanguageParser(source, lexical.tokens, limits).parse()
        val parserDiagnostics = lexical.diagnostics + parsed.diagnostics
        if (parserDiagnostics.hasErrors()) {
            return CompilationResult(source, parserDiagnostics, parsed.program, null)
        }

        val (checked, semanticDiagnostics) = resolveProgram(parsed.program, environment, limits)
        if (checked == null) {
            return CompilationResult(source, parserDiagnostics + semanticDiagnostics, parsed.program, null)
        }

        val lowered = GraphBuilder(parsed.program, checked, limits).build()
        val graph = lowered.graph
            ?: return CompilationResult(
                source,
                parserDiagnostics + lowered.diagnostics,
                parsed.program,
                null,
            )

        return CompilationResult(
            source,
            parserDiagnostics,
            parsed.program,
            TypedModule(source, parsed.program, graph),
        )
    }

    private fun sizeFailure(source: SourceText, message: String): CompilationResult =
        CompilationResult(
            source,
            listOf(
                Diagnostic.error(
                    DiagnosticCode.E_LIMIT,
                    SourceSpan.EMPTY,
                    message,
                ),
            ),
            null,
            null,
        )
}

/** Convenience entry point for callers that do not need to reuse a compiler instance. */
fun compileLanguage(
    sourceName: String,
    text: String,
    environment: LanguageEnvironment,
    limits: LanguageLimits = LanguageLimits.DEFAULT,
): CompilationResult = LangCompiler(environment, limits).compile(sourceName, text)
