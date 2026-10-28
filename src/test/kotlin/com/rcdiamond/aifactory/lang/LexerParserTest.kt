package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexerParserTest {
    private fun lex(text: String): LexResult =
        LanguageLexer(SourceText.of("lex.idsl", text)).lex()

    @Test
    fun `longest punctuation tokens and comments are lexed together`() {
        val tokens = lex("a >> b >= c -- note\n1L 2.5e3 \"s\"").tokens
        assertEquals(
            listOf(
                TokenKind.IDENTIFIER,
                TokenKind.GREATER_GREATER,
                TokenKind.IDENTIFIER,
                TokenKind.GREATER_EQUAL,
                TokenKind.IDENTIFIER,
                TokenKind.LONG,
                TokenKind.DOUBLE,
                TokenKind.STRING,
                TokenKind.EOF,
            ),
            tokens.map { it.kind },
        )
        assertEquals("1L", tokens[5].text)
        assertEquals("2.5e3", tokens[6].text)
        assertEquals("s", tokens[7].stringValue)
    }

    @Test
    fun `arrow is distinct from minus`() {
        val tokens = lex("Fn(Integer) -> Integer - 1").tokens
        assertTrue(tokens.any { it.kind == TokenKind.ARROW })
        assertEquals(1, tokens.count { it.kind == TokenKind.ARROW })
        assertEquals(1, tokens.count { it.kind == TokenKind.MINUS })
    }

    @Test
    fun `json escapes and unicode scalars decode`() {
        val text = "output x : String = \"a\\\"b\\\\c\\n\\t\\u0041\\uD83D\\uDE00\";"
        val result = lex(text)
        assertTrue(result.diagnostics.isEmpty(), result.diagnostics.joinToString())
        val string = result.tokens.single { it.kind == TokenKind.STRING }
        assertEquals("a\"b\\c\n\tA\uD83D\uDE00", string.stringValue)
    }

    @Test
    fun `unpaired surrogate escape is rejected`() {
        val result = lex("output x : String = \"\\uD800\";")
        assertTrue(result.diagnostics.any { it.code == DiagnosticCode.E_SYNTAX })
        assertTrue(result.diagnostics.any { it.message.contains("surrogate") })
    }

    @Test
    fun `unterminated and control characters are rejected`() {
        val unterminated = lex("output x : String = \"abc;")
        assertTrue(unterminated.diagnostics.any { it.code == DiagnosticCode.E_SYNTAX })

        val control = lex("output x : String = \"a\u0001b\";")
        assertTrue(control.diagnostics.any { it.code == DiagnosticCode.E_SYNTAX })
    }

    @Test
    fun `empty source lexes to eof and has no diagnostics`() {
        val result = lex("")
        assertEquals(listOf(TokenKind.EOF), result.tokens.map { it.kind })
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `multiplication binds tighter than addition`() {
        val module = compileTest("output x : Integer = 1 + 2 * 3;").assertSuccess()
        val output = module.program.declarations.single() as OutputDeclaration
        val root = output.expression as Expr.Binary
        assertEquals(BinaryOperator.PLUS, root.operator)
        val right = root.right as Expr.Binary
        assertEquals(BinaryOperator.STAR, right.operator)
    }

    @Test
    fun `application is tighter than subtraction but parentheses change it`() {
        val withParens = compileTest("output x : Integer = identity (-1);").assertSuccess()
        val parenOutput = withParens.program.declarations.single() as OutputDeclaration
        val call = parenOutput.expression as Expr.Call
        assertEquals(1, call.arguments.size)
        assertEquals(-1, (call.arguments.single() as Expr.IntegerLiteral).value)

        val compiled = compileTest("output x : Integer = identity -1;")
        compiled.assertFailure(DiagnosticCode.E_TYPE)
    }

    @Test
    fun `if binds as a complete expression`() {
        val module = compileTest(
            "output x : Integer = if true then 1 else 2;",
        ).assertSuccess()
        val output = module.program.declarations.single() as OutputDeclaration
        assertTrue(output.expression is Expr.If)
    }

    @Test
    fun `chained comparisons are rejected`() {
        compileTest("output x : Boolean = 1 < 2 < 3;")
            .assertFailure(DiagnosticCode.E_SYNTAX)
    }

    @Test
    fun `missing semicolon is a syntax error`() {
        compileTest("output x : Integer = 1").assertFailure(DiagnosticCode.E_SYNTAX)
    }

    @Test
    fun `list trailing comma is rejected`() {
        compileTest("output x : [Integer] = [1, 2,];")
            .assertFailure(DiagnosticCode.E_SYNTAX)
    }

    @Test
    fun `line comments end at the newline and allow a later semicolon`() {
        val module = compileTest("output x : Integer = 1-- trailing text\n;").assertSuccess()
        val output = module.program.declarations.single() as OutputDeclaration
        assertEquals(1, (output.expression as Expr.IntegerLiteral).value)
    }

    @Test
    fun `keyword cannot be used as a name`() {
        compileTest("let input = 1;\noutput x : Integer = 1;")
            .assertFailure(DiagnosticCode.E_SYNTAX)
    }

    @Test
    fun `signed minimum literals are accepted and positive overflow is rejected`() {
        compileTest("output x : Integer = -2147483648;").assertSuccess()
        compileTest("output x : Integer = 2147483648;")
            .assertFailure(DiagnosticCode.E_LITERAL_RANGE)
        compileTest("output x : Long = -9223372036854775808L;").assertSuccess()
        compileTest("output x : Long = 9223372036854775808L;")
            .assertFailure(DiagnosticCode.E_LITERAL_RANGE)
        compileTest("output x : Double = 1e999;")
            .assertFailure(DiagnosticCode.E_LITERAL_RANGE)
    }

    @Test
    fun `block comments are not part of the language`() {
        compileTest("output x : Integer = /* nope */ 1;")
            .assertFailure(DiagnosticCode.E_SYNTAX)
    }
}
