package com.rcdiamond.aifactory.lang

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal fun compileTest(
    source: String,
    environment: LanguageEnvironment = StandardScalarEnvironment.environment,
    limits: LanguageLimits = LanguageLimits.DEFAULT,
    name: String = "test.idsl",
): CompilationResult = LangCompiler(environment, limits).compile(name, source)

internal fun CompilationResult.assertSuccess(): TypedModule {
    assertTrue(success, "expected success but got:\n${renderDiagnostics()}")
    return module ?: error("successful compile without a module")
}

internal fun CompilationResult.assertFailure(code: DiagnosticCode): Diagnostic {
    assertFalse(success, "expected failure with $code but compile succeeded")
    val diagnostic = diagnostics.firstOrNull { it.isError && it.code == code }
    assertTrue(
        diagnostic != null,
        "expected $code but got:\n${renderDiagnostics()}",
    )
    return diagnostic!!
}

internal fun evaluateTest(
    source: String,
    inputs: Map<String, PureValue> = emptyMap(),
    environment: LanguageEnvironment = StandardScalarEnvironment.environment,
    primitives: Map<String, PurePrimitive> = StandardScalarRuntime.primitives,
    limits: LanguageLimits = LanguageLimits.DEFAULT,
    evaluationLimits: EvaluationLimits = EvaluationLimits.DEFAULT,
    name: String = "eval.idsl",
): PureEvaluation {
    val module = LangCompiler(environment, limits).compile(name, source).assertSuccess()
    val result = PureEvaluator(primitives, inputs, evaluationLimits).evaluate(module)
    assertTrue(
        result.success,
        "expected pure evaluation to succeed but got:\n${result.renderDiagnostics(module.source)}",
    )
    return result.result ?: error("successful evaluation without a result")
}

internal fun PureEvaluation.integer(name: String): Int =
    (outputs[name] as? PureValue.IntegerValue)?.value
        ?: error("output '$name' is not Integer: ${outputs[name]}")

internal fun PureEvaluation.long(name: String): Long =
    (outputs[name] as? PureValue.LongValue)?.value
        ?: error("output '$name' is not Long: ${outputs[name]}")

internal fun PureEvaluation.boolean(name: String): Boolean =
    (outputs[name] as? PureValue.BooleanValue)?.value
        ?: error("output '$name' is not Boolean: ${outputs[name]}")

internal fun PureEvaluation.string(name: String): String =
    (outputs[name] as? PureValue.StringValue)?.value
        ?: error("output '$name' is not String: ${outputs[name]}")

internal fun PureEvaluation.list(name: String): List<PureValue> =
    (outputs[name] as? PureValue.ListValue)?.elements
        ?: error("output '$name' is not a List: ${outputs[name]}")

internal fun PureEvaluation.function(name: String): PureValue.FunctionValue =
    (outputs[name] as? PureValue.FunctionValue)
        ?: error("output '$name' is not a function: ${outputs[name]}")

internal fun evaluateFailure(
    source: String,
    inputs: Map<String, PureValue> = emptyMap(),
    environment: LanguageEnvironment = StandardScalarEnvironment.environment,
    primitives: Map<String, PurePrimitive> = StandardScalarRuntime.primitives,
): Diagnostic {
    val module = LangCompiler(environment).compile("eval.idsl", source).assertSuccess()
    val result = PureEvaluator(primitives, inputs).evaluate(module)
    assertFalse(result.success, "expected evaluation failure")
    val diagnostic = result.diagnostics.firstOrNull { it.isError }
    assertTrue(diagnostic != null, "expected an evaluation error")
    return diagnostic!!
}

internal fun Diagnostic.assertEqualsDiagnosticCode(expected: DiagnosticCode) {
    assertEquals(expected, code)
}
