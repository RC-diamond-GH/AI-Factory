package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Consumer-level regressions for the pure backend's resource guards: compile a real program, then
 * evaluate it through [PureEvaluator] with tightened [EvaluationLimits].
 */
class RuntimeSafetyTest {
    @Test
    fun `list concatenation is rejected before materializing past the limit`() {
        val diagnostic = evaluationFailure(
            "output xs : [Integer] = [1] ++ [2];",
            EvaluationLimits(maxListElements = 1),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
        assertTrue(diagnostic.span.end > diagnostic.span.start, "limit diagnostic must point at the concat")
    }

    @Test
    fun `aliased list doubling cannot bypass the materialization limit`() {
        val diagnostic = evaluationFailure(
            """
            let a = [1];
            let b = a ++ a;
            let c = b ++ b;
            let d = c ++ c;
            output xs : [Integer] = d;
            """.trimIndent(),
            EvaluationLimits(maxListElements = 4),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
    }

    @Test
    fun `nested lists are bounded per producing operation`() {
        val diagnostic = evaluationFailure(
            """
            let inner = [1];
            let pair = [inner, inner];
            let quad = pair ++ pair;
            output xs : [[Integer]] = quad;
            """.trimIndent(),
            EvaluationLimits(maxListElements = 3),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
    }

    @Test
    fun `append slice map filter and reduce bound their outputs`() {
        val xs = listOf(1, 2, 3, 4).map { PureValue.IntegerValue(it) }
        val inputs = mapOf("xs" to PureValue.ListValue(xs))

        fun failure(source: String, maxListElements: Int): Diagnostic = evaluationFailure(
            source,
            EvaluationLimits(maxListElements = maxListElements),
            inputs,
        )

        assertEquals(
            DiagnosticCode.E_LIMIT,
            failure("input xs : [Integer];\noutput ys : [Integer] = append xs 9;", 4).code,
        )
        assertEquals(
            DiagnosticCode.E_LIMIT,
            failure("input xs : [Integer];\noutput ys : [Integer] = slice xs 0 2;", 1).code,
        )
        assertEquals(
            DiagnosticCode.E_LIMIT,
            failure("input xs : [Integer];\noutput ys : [Integer] = map (add 1) xs;", 3).code,
        )
        assertEquals(
            DiagnosticCode.E_LIMIT,
            failure("input xs : [Integer];\noutput ys : [Integer] = filter (flip gt 1) xs;", 3).code,
        )
        assertEquals(
            DiagnosticCode.E_LIMIT,
            failure("input xs : [Integer];\noutput total : Integer = reduce add xs 0;", 3).code,
        )
    }

    @Test
    fun `string growth is rejected before concatenating past the limit`() {
        val literal = "x".repeat(8_192)
        val diagnostic = evaluationFailure(
            """
            let s0 = "$literal";
            let s1 = s0 ++ s0;
            let s2 = s1 ++ s1;
            output text : String = s2;
            """.trimIndent(),
            EvaluationLimits(maxStringLength = 16_384),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
        assertEquals(1_048_576, EvaluationLimits.DEFAULT.maxStringLength)
    }

    @Test
    fun `higher order traversal charges evaluation steps`() {
        val xs = List(128) { PureValue.IntegerValue(it) }
        val inputs = mapOf("xs" to PureValue.ListValue(xs))

        val mapped = evaluationFailure(
            "input xs : [Integer];\noutput ys : [Integer] = map (add 1) xs;",
            EvaluationLimits(maxSteps = 32),
            inputs,
        )
        assertEquals(DiagnosticCode.E_LIMIT, mapped.code)

        val reduced = evaluationFailure(
            "input xs : [Integer];\noutput total : Integer = reduce add xs 0;",
            EvaluationLimits(maxSteps = 32),
            inputs,
        )
        assertEquals(DiagnosticCode.E_LIMIT, reduced.code)
    }

    @Test
    fun `exponential composition stops at the step limit instead of applying 2 to the n closures`() {
        val bounded = evaluateTest(
            "let inc = add 1;\noutput value : Integer = inc 0;",
            evaluationLimits = EvaluationLimits(maxSteps = 128),
        )
        assertEquals(1, bounded.integer("value"))

        val diagnostic = evaluationFailure(
            compositionProgram(level = 10),
            EvaluationLimits(maxSteps = 128),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)

        val generous = evaluateTest(
            compositionProgram(level = 10),
            evaluationLimits = EvaluationLimits(maxSteps = 100_000),
        )
        assertEquals(1024, generous.integer("value"))
    }

    @Test
    fun `deep composition evaluation is depth limited`() {
        val shallow = evaluateTest(
            "let inc = add 1;\noutput value : Integer = inc 0;",
            evaluationLimits = EvaluationLimits(maxDepth = 12),
        )
        assertEquals(1, shallow.integer("value"))

        val diagnostic = evaluationFailure(
            compositionProgram(level = 30),
            EvaluationLimits(maxDepth = 12),
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
    }

    @Test
    fun `source returned function values keep charging their budget`() {
        val evaluated = evaluateTest(
            "let inc = add 1;\noutput f : Fn(Integer) -> Integer = inc;",
            evaluationLimits = EvaluationLimits(maxSteps = 24),
        )
        val function = evaluated.function("f")
        val failure = assertFailsWith<RuntimeException> {
            repeat(100) { function.applyTo(PureValue.IntegerValue(1)) }
        }
        assertTrue(
            failure.message.orEmpty().contains("step limit"),
            "expected a step-limit failure, got ${failure.message}",
        )
    }

    @Test
    fun `host function results are still checked for list materialization`() {
        val environment = StandardScalarEnvironment.environment.builder()
            .addPrimitive(
                "makeBig",
                TypeScheme(
                    LangTypes.function(
                        LangTypes.INTEGER,
                        result = LangTypes.list(LangTypes.INTEGER),
                    ),
                ),
            )
            .build()
        val primitives = StandardScalarRuntime.primitives + (
            "makeBig" to PurePrimitive { _ ->
                PureValue.ListValue(List(4) { PureValue.IntegerValue(it) })
            }
            )

        val diagnostic = evaluationFailure(
            "output xs : [Integer] = makeBig 0;",
            EvaluationLimits(maxListElements = 3),
            environment = environment,
            primitives = primitives,
        )
        assertEquals(DiagnosticCode.E_LIMIT, diagnostic.code)
    }

    @Test
    fun `function equality is rejected and ordinary values still compare structurally`() {
        val evaluated = evaluateTest(
            "let inc = add 1;\noutput f : Fn(Integer) -> Integer = inc;",
        )
        val function = evaluated.function("f")

        assertFailsWith<PurePrimitiveException> { valuesEqual(function, function) }
        assertFailsWith<PurePrimitiveException> {
            ScalarOps.comparison(IrComparisonOperator.EQUAL, function, function)
        }
        assertFailsWith<PurePrimitiveException> {
            valuesEqual(
                PureValue.ListValue(listOf(function)),
                PureValue.ListValue(listOf(function)),
            )
        }

        assertTrue(
            valuesEqual(
                PureValue.ListValue(listOf(PureValue.IntegerValue(1), PureValue.IntegerValue(2))),
                PureValue.ListValue(listOf(PureValue.IntegerValue(1), PureValue.IntegerValue(2))),
            ),
        )
    }

    @Test
    fun `double min and max use JVM NaN and signed zero semantics in either order`() {
        val result = evaluateTest(
            """
            output minNaNLeft : Double = min (0.0 / 0.0) 1.0;
            output minNaNRight : Double = min 1.0 (0.0 / 0.0);
            output maxNaNLeft : Double = max (0.0 / 0.0) 1.0;
            output maxNaNRight : Double = max 1.0 (0.0 / 0.0);
            output minZeroLeft : Double = min 0.0 -0.0;
            output minZeroRight : Double = min -0.0 0.0;
            output maxZeroLeft : Double = max 0.0 -0.0;
            output maxZeroRight : Double = max -0.0 0.0;
            """.trimIndent(),
        )

        fun double(name: String): Double =
            (result.outputs[name] as? PureValue.DoubleValue)?.value
                ?: error("output '$name' is not Double")

        assertTrue(double("minNaNLeft").isNaN())
        assertTrue(double("minNaNRight").isNaN())
        assertTrue(double("maxNaNLeft").isNaN())
        assertTrue(double("maxNaNRight").isNaN())
        assertEquals((-0.0).toRawBits(), double("minZeroLeft").toRawBits())
        assertEquals((-0.0).toRawBits(), double("minZeroRight").toRawBits())
        assertEquals(0.0.toRawBits(), double("maxZeroLeft").toRawBits())
        assertEquals(0.0.toRawBits(), double("maxZeroRight").toRawBits())
    }

    private fun compositionProgram(level: Int): String = buildString {
        appendLine("let inc = add 1;")
        appendLine("let p1 = inc >> inc;")
        for (index in 2..level) {
            appendLine("let p$index = p${index - 1} >> p${index - 1};")
        }
        appendLine("output value : Integer = p$level 0;")
    }

    private fun evaluationFailure(
        source: String,
        limits: EvaluationLimits,
        inputs: Map<String, PureValue> = emptyMap(),
        environment: LanguageEnvironment = StandardScalarEnvironment.environment,
        primitives: Map<String, PurePrimitive> = StandardScalarRuntime.primitives,
    ): Diagnostic {
        val module = compileTest(source, environment).assertSuccess()
        val result = PureEvaluator(primitives, inputs, limits).evaluate(module)
        assertFalse(result.success, "expected an evaluation failure but got ${result.result}")
        return result.diagnostics.first { it.isError }
    }
}
