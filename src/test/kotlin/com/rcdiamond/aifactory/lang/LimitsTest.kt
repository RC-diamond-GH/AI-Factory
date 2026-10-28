package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LimitsTest {
    @Test
    fun `source character and byte limits are checked before line indexing`() {
        val characters = compileTest(
            "output x : Integer = 1;",
            limits = LanguageLimits(maxSourceChars = 8),
        )
        characters.assertFailure(DiagnosticCode.E_LIMIT)
        assertNull(characters.module)
        assertEquals(0, characters.source.length)

        val bytes = compileTest(
            "output text : String = \"éééééé\";",
            limits = LanguageLimits(maxSourceBytes = 10),
        )
        bytes.assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `token and string limits stop lexing`() {
        compileTest(
            "output x : Integer = 1;",
            limits = LanguageLimits(maxTokens = 3),
        ).assertFailure(DiagnosticCode.E_LIMIT)

        compileTest(
            "output x : String = \"abcd\";",
            limits = LanguageLimits(maxStringLength = 3),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `expression nesting is depth limited`() {
        val source = "output x : Integer = " + "(".repeat(8) + "1" + ")".repeat(8) + ";"
        compileTest(source, limits = LanguageLimits(maxNestingDepth = 3))
            .assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `long operator chains are depth limited instead of overflowing`() {
        val chain = (1..200).joinToString(" + ") { "1" }
        compileTest(
            "output x : Integer = $chain;",
            limits = LanguageLimits(maxNestingDepth = 16),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `list length and declaration count are limited`() {
        compileTest(
            "output x : [Integer] = [1, 2, 3];",
            limits = LanguageLimits(maxListLength = 2),
        ).assertFailure(DiagnosticCode.E_LIMIT)

        compileTest(
            """
            output a : Integer = 1;
            output b : Integer = 2;
            """.trimIndent(),
            limits = LanguageLimits(maxDeclarations = 1),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `type depth is limited during annotation checking`() {
        compileTest(
            """
            let deep : [[[Integer]]] = [[[]]];
            output x : Integer = 1;
            """.trimIndent(),
            limits = LanguageLimits(maxTypeDepth = 2),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `dependency depth is limited without stack overflow`() {
        compileTest(
            """
            let a = b;
            let b = c;
            let c = d;
            let d = 1;
            output result : Integer = a;
            """.trimIndent(),
            limits = LanguageLimits(maxDependencyDepth = 2),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `graph node count is limited during lowering`() {
        compileTest(
            "output x : Integer = 1 + 2;",
            limits = LanguageLimits(maxNodes = 1),
        ).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `evaluation depth and step limits return diagnostics`() {
        val module = compileTest("output x : Integer = 1 + 2;").assertSuccess()

        val depth = PureEvaluator(limits = EvaluationLimits(maxDepth = 1)).evaluate(module)
        assertFalse(depth.success)
        assertEquals(DiagnosticCode.E_LIMIT, depth.diagnostics.first().code)

        val steps = PureEvaluator(limits = EvaluationLimits(maxSteps = 1)).evaluate(module)
        assertFalse(steps.success)
        assertEquals(DiagnosticCode.E_LIMIT, steps.diagnostics.first().code)
    }

    @Test
    fun `evaluation list materialization is limited`() {
        val module = compileTest("output xs : [Integer] = [1, 2];").assertSuccess()
        val result = PureEvaluator(limits = EvaluationLimits(maxListElements = 1)).evaluate(module)
        assertFalse(result.success)
        assertEquals(DiagnosticCode.E_LIMIT, result.diagnostics.first().code)
    }
}
