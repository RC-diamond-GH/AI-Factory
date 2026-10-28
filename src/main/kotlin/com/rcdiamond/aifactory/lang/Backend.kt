package com.rcdiamond.aifactory.lang

/**
 * One backend run. [result] is null when a backend error occurred; warnings may coexist with a
 * result. Diagnostics use the same codes and source spans as the frontend.
 */
data class BackendResult<R>(
    val result: R?,
    val diagnostics: List<Diagnostic>,
) {
    val success: Boolean get() = result != null && diagnostics.none { it.isError }

    fun renderDiagnostics(source: SourceText): String =
        diagnostics.joinToString("\n") { it.render(source) }

    companion object {
        fun <R> success(result: R, diagnostics: List<Diagnostic> = emptyList()): BackendResult<R> =
            BackendResult(result, diagnostics)

        fun <R> failure(diagnostics: List<Diagnostic>): BackendResult<R> =
            BackendResult(null, diagnostics)
    }
}

/**
 * Extensible typed-graph consumer. A DSL backend maps [IrOperation] values and primitive names to
 * native definitions; the pure backend in this package evaluates them without any game types.
 *
 * Backends capture their own context (input bindings, native catalogs, runtime values, limits) in
 * the implementation constructor.
 */
fun interface TypedBackend<R> {
    fun lower(module: TypedModule): BackendResult<R>
}

fun <R> TypedModule.lowerWith(backend: TypedBackend<R>): BackendResult<R> = backend.lower(this)
