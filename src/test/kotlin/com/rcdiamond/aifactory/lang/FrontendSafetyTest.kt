package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrontendSafetyTest {
    @Test
    fun `parenthesized operator chains cannot multiply into unbounded AST depth`() {
        var expression = "1"
        repeat(64) { expression = "(" + expression + " + 1".repeat(127) + ")" }
        val source = "output result : Integer = $expression;"
        assertTrue(
            source.length <= 32_768,
            "repro must fit the 32 KiB consumer profile, was ${source.length} characters",
        )
        compileTest(source).assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `total structural depth is limited independently of grammar nesting`() {
        var expression = "1"
        repeat(4) { expression = "($expression + 1 + 1)" }
        val source = "output x : Integer = $expression;"

        // Parentheses and each operator chain fit maxNestingDepth = 8, but the folded structure is
        // nine nodes deep. The structural pass must reject it.
        compileTest(source, limits = LanguageLimits(maxNestingDepth = 8))
            .assertFailure(DiagnosticCode.E_LIMIT)

        compileTest(source, limits = LanguageLimits(maxNestingDepth = 9)).assertSuccess()
    }

    @Test
    fun `unused let expressions are depth checked`() {
        var expression = "1"
        repeat(4) { expression = "($expression + 1 + 1)" }
        val source = """
            let unused : Integer = $expression;
            output result : Integer = 1;
        """.trimIndent()

        compileTest(source, limits = LanguageLimits(maxNestingDepth = 8))
            .assertFailure(DiagnosticCode.E_LIMIT)
    }

    @Test
    fun `deep but accepted expressions compile through checking and lowering`() {
        var expression = "1"
        repeat(60) { expression = "($expression + 1)" }
        val module = compileTest("output x : Integer = $expression;").assertSuccess()
        assertEquals("Integer", module.output("x")?.type?.display())
    }

    @Test
    fun `unknown name diagnostics stop at the configured cap`() {
        val arguments = (0 until 200).joinToString(" ") { "missing$it" }
        val result = compileTest(
            "output result : Integer = root $arguments;",
            limits = LanguageLimits(maxDiagnostics = 4),
        )

        result.assertFailure(DiagnosticCode.E_UNKNOWN_NAME)
        assertEquals(4, result.diagnostics.size)
        assertEquals(
            4,
            result.diagnostics.count { it.code == DiagnosticCode.E_UNKNOWN_NAME },
        )
    }

    @Test
    fun `function values have no default equality`() {
        compileTest("let f = add 1; output same : Boolean = f == f;")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest("output same : Boolean = (add 1) == (add 1);")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest("output same : Boolean = eq (add 1) (add 1);")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest("output same : Boolean = ne (add 1) (add 1);")
            .assertFailure(DiagnosticCode.E_TYPE)
    }

    @Test
    fun `lists containing functions have no default equality`() {
        compileTest("output same : Boolean = [add 1] == [add 1];")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest("output same : Boolean = eq [add 1] [add 1];")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest(
            """
            let f = add 1;
            output same : Boolean = [[f]] == [[f]];
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)
    }

    @Test
    fun `scalar, data and scalar list equality remain available`() {
        compileTest(
            """
            output integers : Boolean = 1 == 1;
            output longs : Boolean = 1L == 1L;
            output doubles : Boolean = 1.0 == 1.0;
            output strings : Boolean = "a" == "a";
            output lists : Boolean = [1, 2] == [1, 2];
            output primitives : Boolean = eq 1 1 && ne 2 3;
            """.trimIndent(),
        ).assertSuccess()

        val environment = LanguageEnvironment.basic().builder()
            .addNominalType("Token")
            .addPrimitive("token", TypeScheme(LangTypes.nominal("Token")))
            .build()
        compileTest(
            """
            output same : Boolean = token == token;
            output sameList : Boolean = [token] == [token];
            """.trimIndent(),
            environment,
        ).assertSuccess()
    }
}
