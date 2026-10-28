package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeCheckTest {
    @Test
    fun `forward references and output port namespace work`() {
        val module = compileTest(
            """
            output result : Integer = later;
            let later = 41 + 1;
            """.trimIndent(),
        ).assertSuccess()
        assertEquals(LangTypes.INTEGER, module.output("result")?.type)
    }

    @Test
    fun `output port may share a binding name`() {
        compileTest(
            """
            let result = 1;
            output result : Integer = result;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `immediate cycles report the chain`() {
        val diagnostic = compileTest(
            """
            let a = b;
            let b = c;
            let c = a;
            output result : Integer = a;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_CYCLE)
        assertTrue(diagnostic.message.contains("a -> b -> c -> a"))
        assertTrue(diagnostic.related.isNotEmpty())
    }

    @Test
    fun `self cycle is rejected`() {
        compileTest(
            """
            let a = a;
            output result : Integer = a;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_CYCLE)
    }

    @Test
    fun `duplicate names and shadowed primitives are rejected`() {
        compileTest(
            """
            let a = 1;
            input a : Integer;
            output result : Integer = a;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_DUPLICATE_NAME)

        compileTest(
            """
            output result : Integer = 1;
            output result : Integer = 2;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_DUPLICATE_NAME)

        compileTest(
            """
            let add = 1;
            output result : Integer = add;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_DUPLICATE_NAME)
    }

    @Test
    fun `unknown names, missing output and unknown types are diagnosed`() {
        compileTest("output result : Integer = missing;")
            .assertFailure(DiagnosticCode.E_UNKNOWN_NAME)
        compileTest("let a = 1;")
            .assertFailure(DiagnosticCode.E_SYNTAX)
        compileTest("output result : Missing = 1;")
            .assertFailure(DiagnosticCode.E_UNKNOWN_TYPE)
    }

    @Test
    fun `unspecialized primitives and empty lists are rejected`() {
        compileTest("output result = eq;")
            .assertFailure(DiagnosticCode.E_UNRESOLVED_TYPE)
        compileTest(
            """
            let nothing = [];
            output result : Integer = 1;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_EMPTY_LIST_TYPE)

        compileTest(
            """
            let nothing : [Integer] = [];
            output result : [Integer] = nothing;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `let is monomorphic and cannot be reused at two types`() {
        compileTest(
            """
            input flag : Boolean;
            let same = eq;
            output first : Boolean = same 1 1;
            output second : Boolean = same flag flag;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)

        compileTest(
            """
            input flag : Boolean;
            let same = eq;
            output first : Boolean = same 1 1;
            output second : Boolean = same 2 2;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `annotations check but do not convert`() {
        compileTest(
            """
            output bad : Integer = 1L;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)

        compileTest(
            """
            let same : Fn(Integer, Integer) -> Boolean = eq;
            output result : Boolean = same 1 2;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `numeric operations require one concrete numeric type`() {
        compileTest(
            """
            input energy : Long;
            output bad : Boolean = energy < 1000;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)

        compileTest(
            """
            input energy : Long;
            input limit : Long;
            output good : Boolean = energy < limit;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `conditional branches and boolean operands must agree`() {
        compileTest(
            """
            input flag : Boolean;
            output bad : Integer = if flag then 1 else "x";
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)

        compileTest(
            """
            output bad : Boolean = 1 && true;
            """.trimIndent(),
        ).assertFailure(DiagnosticCode.E_TYPE)

        compileTest(
            """
            input flag : Boolean;
            output good : Integer = if flag then 1 else 2;
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `concat selects string or homogeneous list and rejects numbers`() {
        compileTest("output bad : Integer = 1 ++ 2;")
            .assertFailure(DiagnosticCode.E_TYPE)
        compileTest(
            """
            output text : String = "a" ++ "b";
            output numbers : [Integer] = [1] ++ [2];
            """.trimIndent(),
        ).assertSuccess()
    }

    @Test
    fun `application arity and composition signatures are checked`() {
        compileTest("output bad : Integer = identity 1 2;")
            .assertFailure(DiagnosticCode.E_NOT_CALLABLE)
        compileTest("output bad : Fn(Integer) -> Integer = add >> increment;")
            .assertFailure(DiagnosticCode.E_FN_SIGNATURE)
        compileTest("let f : Fn(Integer) -> Integer = add; output x : Integer = f 1;")
            .assertFailure(DiagnosticCode.E_FN_SIGNATURE)
    }

    @Test
    fun `partial application has a function type and can be completed`() {
        val module = compileTest(
            """
            input base : Integer;
            let inc = add base;
            output result : Integer = inc 1;
            output function : Fn(Integer) -> Integer = inc;
            """.trimIndent(),
        ).assertSuccess()
        assertEquals("Fn(Integer) -> Integer", module.output("function")?.type?.display())
        assertEquals(LangTypes.INTEGER, module.output("result")?.type)
    }

    @Test
    fun `composition of unary functions is accepted`() {
        val module = compileTest(
            """
            let inc = add 1;
            let twice = mul 2;
            output function : Fn(Integer) -> Integer = inc >> twice;
            """.trimIndent(),
        ).assertSuccess()
        assertEquals("Fn(Integer) -> Integer", module.output("function")?.type?.display())
    }

    @Test
    fun `type diagnostics carry the source location`() {
        val source = "input energy : Long;\noutput bad : Boolean = energy < 1000;"
        val result = compileTest(source)
        val diagnostic = result.assertFailure(DiagnosticCode.E_TYPE)
        val position = result.source.positionAt(diagnostic.span.start)
        assertEquals(2, position.line)
        assertTrue(position.column > 1)
    }
}
