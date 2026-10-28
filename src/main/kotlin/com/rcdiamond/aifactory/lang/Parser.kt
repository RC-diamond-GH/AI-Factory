package com.rcdiamond.aifactory.lang

import java.math.BigInteger

data class ParseResult(val program: Program, val diagnostics: List<Diagnostic>)

/**
 * Recursive-descent parser for the shared language grammar.
 *
 * Expression and type recursion is depth-limited. Right-associative operator chains are collected
 * iteratively and folded, so a long `a && b && ...` chain cannot overflow the stack. A syntax error
 * abandons the current declaration, records one diagnostic and resynchronizes at the next `;`.
 */
class LanguageParser(
    private val source: SourceText,
    private val tokens: List<Token>,
    private val limits: LanguageLimits = LanguageLimits.DEFAULT,
) {
    private var index = 0
    private var depth = 0
    private val diagnostics = ArrayList<Diagnostic>()

    fun parse(): ParseResult {
        val declarations = ArrayList<Declaration>()
        if (tokens.isEmpty() || tokens.last().kind != TokenKind.EOF) {
            addError(
                Diagnostic.error(
                    DiagnosticCode.E_SYNTAX,
                    SourceSpan.at(source.length),
                    "token stream is missing the EOF token",
                ),
            )
            return ParseResult(Program(emptyList(), SourceSpan.EMPTY), diagnostics)
        }

        while (!check(TokenKind.EOF)) {
            if (declarations.size >= limits.maxDeclarations) {
                addError(
                    Diagnostic.error(
                        DiagnosticCode.E_LIMIT,
                        current().span,
                        "declaration count exceeds the limit ${limits.maxDeclarations}",
                    ),
                )
                break
            }
            val before = index
            try {
                declarations.add(parseDeclaration())
            } catch (failure: ParseAbort) {
                addError(failure.diagnostic)
                synchronize()
            }
            if (index == before) {
                // Defensive progress guarantee for malformed token streams.
                index++
            }
        }

        val span = if (declarations.isEmpty()) {
            SourceSpan.EMPTY
        } else {
            SourceSpan(declarations.first().span.start, declarations.last().span.end)
        }
        return ParseResult(Program(declarations, span), diagnostics)
    }

    private fun parseDeclaration(): Declaration {
        val start = current().span.start
        return when (current().kind) {
            TokenKind.INPUT -> parseInput(start)
            TokenKind.LET -> parseLet(start)
            TokenKind.OUTPUT -> parseOutput(start)
            else -> throw abort(
                DiagnosticCode.E_SYNTAX,
                current().span,
                "expected 'input', 'let' or 'output' declaration, found ${describe(current())}",
            )
        }
    }

    private fun parseInput(start: Int): InputDeclaration {
        advance() // input
        val name = expectIdentifier("input name")
        expect(TokenKind.COLON, "expected ':' after input name '${name.text}'")
        val type = parseType()
        val semicolon = expect(TokenKind.SEMICOLON, "expected ';' after input declaration")
        return InputDeclaration(name.text, name.span, type, SourceSpan(start, semicolon.span.end))
    }

    private fun parseLet(start: Int): LetDeclaration {
        advance() // let
        val name = expectIdentifier("let name")
        val annotation = if (match(TokenKind.COLON)) parseType() else null
        expect(TokenKind.EQUALS, "expected '=' after let name '${name.text}'")
        val expression = parseExpression()
        val semicolon = expect(TokenKind.SEMICOLON, "expected ';' after let declaration")
        return LetDeclaration(
            name.text,
            name.span,
            annotation,
            expression,
            SourceSpan(start, semicolon.span.end),
        )
    }

    private fun parseOutput(start: Int): OutputDeclaration {
        advance() // output
        val name = expectIdentifier("output port name")
        val annotation = if (match(TokenKind.COLON)) parseType() else null
        expect(TokenKind.EQUALS, "expected '=' after output name '${name.text}'")
        val expression = parseExpression()
        val semicolon = expect(TokenKind.SEMICOLON, "expected ';' after output declaration")
        return OutputDeclaration(
            name.text,
            name.span,
            annotation,
            expression,
            SourceSpan(start, semicolon.span.end),
        )
    }

    private fun parseType(): TypeRef = nested(current().span) {
        val start = current().span.start
        when (current().kind) {
            TokenKind.FN -> {
                advance()
                expect(TokenKind.LPAREN, "expected '(' after 'Fn'")
                val parameters = ArrayList<TypeRef>()
                if (!check(TokenKind.RPAREN)) {
                    parameters.add(parseType())
                    while (match(TokenKind.COMMA)) parameters.add(parseType())
                }
                expect(TokenKind.RPAREN, "expected ')' in function type")
                expect(TokenKind.ARROW, "expected '->' after function type parameters")
                val result = parseType()
                TypeRef.Function(parameters, result, SourceSpan(start, result.span.end))
            }

            TokenKind.LBRACKET -> {
                advance()
                val element = parseType()
                val close = expect(TokenKind.RBRACKET, "expected ']' to close list type")
                TypeRef.ListOf(element, SourceSpan(start, close.span.end))
            }

            TokenKind.IDENTIFIER -> {
                val token = advance()
                TypeRef.Named(token.text, token.span)
            }

            else -> throw abort(
                DiagnosticCode.E_SYNTAX,
                current().span,
                "expected a type name, '[' or 'Fn', found ${describe(current())}",
            )
        }
    }

    private fun parseExpression(): Expr = nested(current().span) {
        if (check(TokenKind.IF)) parseIf() else parseOr()
    }

    private fun parseIf(): Expr {
        val ifToken = advance()
        val condition = parseExpression()
        expect(TokenKind.THEN, "expected 'then' in conditional expression")
        val thenBranch = parseExpression()
        expect(TokenKind.ELSE, "expected 'else' in conditional expression")
        val elseBranch = parseExpression()
        return Expr.If(condition, thenBranch, elseBranch, SourceSpan(ifToken.span.start, elseBranch.span.end))
    }

    private fun parseOr(): Expr {
        val operands = ArrayList<Expr>()
        operands.add(parseAnd())
        while (match(TokenKind.OR_OR)) {
            operands.add(parseAnd())
            guardChain(operands.size)
        }
        return foldRight(operands, BinaryOperator.OR)
    }

    private fun parseAnd(): Expr {
        val operands = ArrayList<Expr>()
        operands.add(parseCompare())
        while (match(TokenKind.AND_AND)) {
            operands.add(parseCompare())
            guardChain(operands.size)
        }
        return foldRight(operands, BinaryOperator.AND)
    }

    private fun parseCompare(): Expr {
        val left = parseCompose()
        val operator = comparisonOperator(current().kind) ?: return left
        advance()
        val right = parseCompose()
        if (comparisonOperator(current().kind) != null) {
            throw abort(
                DiagnosticCode.E_SYNTAX,
                current().span,
                "comparison operators cannot be chained; use '&&' to combine comparisons",
            )
        }
        return Expr.Binary(
            operator,
            left,
            right,
            SourceSpan(left.span.start, right.span.end),
        )
    }

    private fun parseCompose(): Expr {
        val operands = ArrayList<Expr>()
        operands.add(parseConcat())
        while (match(TokenKind.GREATER_GREATER)) {
            operands.add(parseConcat())
            guardChain(operands.size)
        }
        return foldRight(operands, BinaryOperator.COMPOSE)
    }

    private fun parseConcat(): Expr {
        val operands = ArrayList<Expr>()
        operands.add(parseAdd())
        while (match(TokenKind.PLUSPLUS)) {
            operands.add(parseAdd())
            guardChain(operands.size)
        }
        return foldRight(operands, BinaryOperator.CONCAT)
    }

    private fun parseAdd(): Expr {
        var left = parseMul()
        var operators = 0
        while (check(TokenKind.PLUS) || check(TokenKind.MINUS)) {
            val token = advance()
            val operator = if (token.kind == TokenKind.PLUS) BinaryOperator.PLUS else BinaryOperator.MINUS
            val right = parseMul()
            left = Expr.Binary(operator, left, right, SourceSpan(left.span.start, right.span.end))
            guardChain(++operators)
        }
        return left
    }

    private fun parseMul(): Expr {
        var left = parseUnary()
        var operators = 0
        while (check(TokenKind.STAR) || check(TokenKind.SLASH) || check(TokenKind.PERCENT)) {
            val token = advance()
            val operator = when (token.kind) {
                TokenKind.STAR -> BinaryOperator.STAR
                TokenKind.SLASH -> BinaryOperator.SLASH
                else -> BinaryOperator.PERCENT
            }
            val right = parseUnary()
            left = Expr.Binary(operator, left, right, SourceSpan(left.span.start, right.span.end))
            guardChain(++operators)
        }
        return left
    }

    private fun parseUnary(): Expr {
        if (!check(TokenKind.MINUS)) return parseApplication()
        val minus = advance()
        val next = current()
        if (next.kind == TokenKind.INTEGER || next.kind == TokenKind.LONG || next.kind == TokenKind.DOUBLE) {
            advance()
            return parseNumericLiteral(next, negative = true, span = SourceSpan(minus.span.start, next.span.end))
        }
        return nested(minus.span) {
            val operand = parseUnary()
            Expr.Negate(operand, SourceSpan(minus.span.start, operand.span.end))
        }
    }

    private fun parseApplication(): Expr {
        val start = current().span.start
        val callee = parseAtom()
        val arguments = ArrayList<Expr>()
        while (startsAtom(current().kind)) arguments.add(parseAtom())
        if (arguments.isEmpty()) return callee
        return Expr.Call(
            callee,
            arguments,
            SourceSpan(start, arguments.last().span.end),
        )
    }

    private fun parseAtom(): Expr {
        val token = current()
        return when (token.kind) {
            TokenKind.TRUE -> {
                advance()
                Expr.BooleanLiteral(true, token.span)
            }

            TokenKind.FALSE -> {
                advance()
                Expr.BooleanLiteral(false, token.span)
            }

            TokenKind.STRING -> {
                advance()
                Expr.StringLiteral(token.stringValue ?: "", token.span)
            }

            TokenKind.INTEGER, TokenKind.LONG, TokenKind.DOUBLE -> {
                advance()
                parseNumericLiteral(token, negative = false, span = token.span)
            }

            TokenKind.IDENTIFIER -> {
                advance()
                Expr.Name(token.text, token.span)
            }

            TokenKind.LPAREN -> {
                advance()
                if (check(TokenKind.RPAREN)) {
                    throw abort(
                        DiagnosticCode.E_SYNTAX,
                        current().span,
                        "empty parentheses are not a value; the language has no unit literal",
                    )
                }
                val inner = parseExpression()
                expect(TokenKind.RPAREN, "expected ')' to close parenthesized expression")
                inner
            }

            TokenKind.LBRACKET -> parseList()
            TokenKind.ITEM, TokenKind.BLOCK, TokenKind.NBT -> parseSingleArgumentObject(token)
            TokenKind.FLUID -> parseFluid()
            TokenKind.OP -> parseOperatorReference()
            else -> throw abort(
                DiagnosticCode.E_SYNTAX,
                token.span,
                "expected an expression, found ${describe(token)}",
            )
        }
    }

    private fun parseList(): Expr {
        val start = current().span.start
        advance() // [
        val elements = ArrayList<Expr>()
        if (!check(TokenKind.RBRACKET)) {
            while (true) {
                if (elements.size >= limits.maxListLength) {
                    throw abort(
                        DiagnosticCode.E_LIMIT,
                        current().span,
                        "list literal exceeds the element limit ${limits.maxListLength}",
                    )
                }
                elements.add(parseExpression())
                if (!match(TokenKind.COMMA)) break
                if (check(TokenKind.RBRACKET)) {
                    throw abort(
                        DiagnosticCode.E_SYNTAX,
                        current().span,
                        "trailing comma is not allowed in a list literal",
                    )
                }
            }
        }
        val close = expect(TokenKind.RBRACKET, "expected ']' to close list literal")
        return Expr.ListLiteral(elements, SourceSpan(start, close.span.end))
    }

    private fun parseSingleArgumentObject(keyword: Token): Expr {
        advance()
        val kind = when (keyword.kind) {
            TokenKind.ITEM -> ObjectKind.ITEM
            TokenKind.BLOCK -> ObjectKind.BLOCK
            else -> ObjectKind.NBT
        }
        val text = expect(TokenKind.STRING, "expected a string literal after '${keyword.text}'")
        return Expr.Construct(
            kind,
            listOf(ConstructorArgument.Text(text.stringValue ?: "", text.span)),
            SourceSpan(keyword.span.start, text.span.end),
        )
    }

    private fun parseFluid(): Expr {
        val keyword = advance()
        val id = expect(TokenKind.STRING, "expected a fluid resource string after 'fluid'")
        val amountToken = expect(TokenKind.INTEGER, "expected a positive Integer literal as the fluid amount")
        val amount = parseFluidAmount(amountToken)
        return Expr.Construct(
            ObjectKind.FLUID,
            listOf(
                ConstructorArgument.Text(id.stringValue ?: "", id.span),
                ConstructorArgument.Integer(amount, amountToken.span),
            ),
            SourceSpan(keyword.span.start, amountToken.span.end),
        )
    }

    private fun parseOperatorReference(): Expr {
        val keyword = advance()
        val id = expect(TokenKind.STRING, "expected an operator resource string after 'op'")
        return Expr.OperatorReference(id.stringValue ?: "", SourceSpan(keyword.span.start, id.span.end))
    }

    private fun parseNumericLiteral(token: Token, negative: Boolean, span: SourceSpan): Expr {
        val raw = if (token.kind == TokenKind.DOUBLE) token.text.removeSuffix("L") else token.text
        if (token.kind == TokenKind.DOUBLE) {
            val parsed = (if (negative) "-$raw" else raw).toDoubleOrNull()
            if (parsed == null || !parsed.isFinite()) {
                addError(
                    Diagnostic.error(
                        DiagnosticCode.E_LITERAL_RANGE,
                        span,
                        "Double literal '$raw' is not a finite Double",
                    ),
                )
                val fallback = if (negative) -Double.MAX_VALUE else Double.MAX_VALUE
                return Expr.DoubleLiteral(fallback, span)
            }
            return Expr.DoubleLiteral(parsed, span)
        }

        val digits = if (token.kind == TokenKind.LONG) raw.removeSuffix("L") else raw
        val magnitude = try {
            BigInteger(digits)
        } catch (_: NumberFormatException) {
            addError(
                Diagnostic.error(DiagnosticCode.E_LITERAL_RANGE, span, "invalid numeric literal '$raw'"),
            )
            BigInteger.ZERO
        }
        val signed = if (negative) magnitude.negate() else magnitude

        return if (token.kind == TokenKind.LONG) {
            if (signed < LONG_MIN || signed > LONG_MAX) {
                addError(
                    Diagnostic.error(
                        DiagnosticCode.E_LITERAL_RANGE,
                        span,
                        "Long literal '$raw' is outside the signed 64-bit range",
                    ),
                )
            }
            Expr.LongLiteral(
                clamp(signed, LONG_MIN, LONG_MAX).toLong(),
                span,
            )
        } else {
            if (signed < INT_MIN || signed > INT_MAX) {
                addError(
                    Diagnostic.error(
                        DiagnosticCode.E_LITERAL_RANGE,
                        span,
                        "Integer literal '$raw' is outside the signed 32-bit range",
                    ),
                )
            }
            Expr.IntegerLiteral(
                clamp(signed, INT_MIN, INT_MAX).toInt(),
                span,
            )
        }
    }

    private fun parseFluidAmount(token: Token): Int {
        val value = try {
            BigInteger(token.text)
        } catch (_: NumberFormatException) {
            addError(Diagnostic.error(DiagnosticCode.E_LITERAL_RANGE, token.span, "invalid fluid amount"))
            return 1
        }
        if (value < BigInteger.ONE || value > INT_MAX) {
            addError(
                Diagnostic.error(
                    DiagnosticCode.E_LITERAL_RANGE,
                    token.span,
                    "fluid amount must be a positive Integer, found ${token.text}",
                ),
            )
            return clamp(value, BigInteger.ONE, INT_MAX).toInt()
        }
        return value.toInt()
    }

    /**
     * Right-associative chains are folded iteratively during parsing, but the resulting AST is
     * nested. Cap chain length so later recursive type checking/lowering cannot overflow the stack
     * even when token limits are generous.
     */
    private fun guardChain(length: Int) {
        if (length > limits.maxNestingDepth) {
            throw abort(
                DiagnosticCode.E_LIMIT,
                current().span,
                "operator chain depth exceeds the limit ${limits.maxNestingDepth}",
            )
        }
    }

    private fun foldRight(operands: List<Expr>, operator: BinaryOperator): Expr {
        var result = operands.last()
        for (position in operands.size - 2 downTo 0) {
            val left = operands[position]
            result = Expr.Binary(operator, left, result, SourceSpan(left.span.start, result.span.end))
        }
        return result
    }

    private fun comparisonOperator(kind: TokenKind): BinaryOperator? = when (kind) {
        TokenKind.EQUAL_EQUAL -> BinaryOperator.EQUAL
        TokenKind.NOT_EQUAL -> BinaryOperator.NOT_EQUAL
        TokenKind.LESS -> BinaryOperator.LESS
        TokenKind.LESS_EQUAL -> BinaryOperator.LESS_EQUAL
        TokenKind.GREATER -> BinaryOperator.GREATER
        TokenKind.GREATER_EQUAL -> BinaryOperator.GREATER_EQUAL
        else -> null
    }

    private fun startsAtom(kind: TokenKind): Boolean = when (kind) {
        TokenKind.IDENTIFIER,
        TokenKind.TRUE,
        TokenKind.FALSE,
        TokenKind.STRING,
        TokenKind.INTEGER,
        TokenKind.LONG,
        TokenKind.DOUBLE,
        TokenKind.LPAREN,
        TokenKind.LBRACKET,
        TokenKind.OP,
        TokenKind.ITEM,
        TokenKind.BLOCK,
        TokenKind.FLUID,
        TokenKind.NBT -> true

        else -> false
    }

    private fun current(): Token = tokens[index.coerceAtMost(tokens.size - 1)]

    private fun check(kind: TokenKind): Boolean = current().kind == kind

    private fun advance(): Token {
        val token = current()
        if (index < tokens.size - 1) index++
        return token
    }

    private fun match(kind: TokenKind): Boolean {
        if (!check(kind)) return false
        advance()
        return true
    }

    private fun expect(kind: TokenKind, message: String): Token {
        if (check(kind)) return advance()
        throw abort(DiagnosticCode.E_SYNTAX, current().span, "$message, found ${describe(current())}")
    }

    private fun expectIdentifier(what: String): Token {
        if (check(TokenKind.IDENTIFIER)) return advance()
        throw abort(
            DiagnosticCode.E_SYNTAX,
            current().span,
            "expected $what, found ${describe(current())}",
        )
    }

    private fun synchronize() {
        while (!check(TokenKind.EOF) && !check(TokenKind.SEMICOLON)) index++
        if (check(TokenKind.SEMICOLON)) index++
    }

    private fun <T> nested(span: SourceSpan, block: () -> T): T {
        depth++
        if (depth > limits.maxNestingDepth) {
            depth--
            throw abort(
                DiagnosticCode.E_LIMIT,
                span,
                "nesting depth exceeds the limit ${limits.maxNestingDepth}",
            )
        }
        return try {
            block()
        } finally {
            depth--
        }
    }

    private fun abort(code: DiagnosticCode, span: SourceSpan, message: String): ParseAbort =
        ParseAbort(Diagnostic.error(code, span, message))

    private fun clamp(value: BigInteger, minimum: BigInteger, maximum: BigInteger): BigInteger = when {
        value < minimum -> minimum
        value > maximum -> maximum
        else -> value
    }

    private fun addError(diagnostic: Diagnostic) {
        if (diagnostics.size >= limits.maxDiagnostics) return
        diagnostics.add(diagnostic)
    }

    private fun describe(token: Token): String = when (token.kind) {
        TokenKind.EOF -> "end of file"
        TokenKind.STRING -> "string literal"
        TokenKind.IDENTIFIER -> "identifier '${token.text}'"
        else -> "'${token.text}'"
    }

    private class ParseAbort(val diagnostic: Diagnostic) : RuntimeException(diagnostic.message)

    companion object {
        private val INT_MIN: BigInteger = BigInteger.valueOf(Int.MIN_VALUE.toLong())
        private val INT_MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())
        private val LONG_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE)
        private val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    }
}

/** Convenience entry point that lexes and parses in one step. */
fun parseLanguageSource(
    sourceName: String,
    text: String,
    limits: LanguageLimits = LanguageLimits.DEFAULT,
): Pair<ParseResult?, List<Diagnostic>> {
    val source = SourceText.of(sourceName, text)
    val lexed = LanguageLexer(source, limits).lex()
    if (lexed.diagnostics.hasErrors()) return null to lexed.diagnostics
    val parsed = LanguageParser(source, lexed.tokens, limits).parse()
    return parsed to (lexed.diagnostics + parsed.diagnostics)
}
