package com.rcdiamond.aifactory.lang

enum class TokenKind {
    IDENTIFIER,
    INTEGER,
    LONG,
    DOUBLE,
    STRING,

    INPUT,
    LET,
    OUTPUT,
    IF,
    THEN,
    ELSE,
    TRUE,
    FALSE,
    OP,
    ITEM,
    BLOCK,
    FLUID,
    NBT,
    FN,

    COLON,
    SEMICOLON,
    EQUALS,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    COMMA,
    ARROW,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    PLUSPLUS,
    GREATER,
    GREATER_GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL,
    EQUAL_EQUAL,
    NOT_EQUAL,
    AND_AND,
    OR_OR,

    EOF,
}

/**
 * A lexical token. [text] is the exact source slice. [stringValue] is the decoded value for
 * [TokenKind.STRING]; numeric tokens keep their source spelling so the parser can apply signed
 * range checks without losing precision.
 */
data class Token(
    val kind: TokenKind,
    val span: SourceSpan,
    val text: String,
    val stringValue: String? = null,
)

data class LexResult(val tokens: List<Token>, val diagnostics: List<Diagnostic>)

/**
 * UTF-16-offset lexer for the shared language subset.
 *
 * The lexer always returns an EOF token and never throws for malformed input. Lexical diagnostics
 * are returned alongside tokens; callers must not continue to semantic analysis when one is an
 * error.
 */
class LanguageLexer(
    private val source: SourceText,
    private val limits: LanguageLimits = LanguageLimits.DEFAULT,
) {
    private val text = source.text
    private var index = 0
    private val tokens = ArrayList<Token>()
    private val diagnostics = ArrayList<Diagnostic>()

    fun lex(): LexResult {
        if (text.length > limits.maxSourceChars) {
            addError(
                SourceSpan(0, text.length),
                "source length ${text.length} exceeds the UTF-16 limit ${limits.maxSourceChars}",
                DiagnosticCode.E_LIMIT,
            )
            tokens.add(Token(TokenKind.EOF, SourceSpan.at(text.length), ""))
            return LexResult(tokens, diagnostics)
        }
        if (source.byteLength > limits.maxSourceBytes) {
            addError(
                SourceSpan(0, text.length),
                "source is ${source.byteLength} UTF-8 bytes, exceeding the limit ${limits.maxSourceBytes}",
                DiagnosticCode.E_LIMIT,
            )
            tokens.add(Token(TokenKind.EOF, SourceSpan.at(text.length), ""))
            return LexResult(tokens, diagnostics)
        }

        while (true) {
            skipWhitespaceAndComments()
            if (index >= text.length) break
            if (tokens.size >= limits.maxTokens) {
                addError(SourceSpan.at(index), "token count exceeds the limit ${limits.maxTokens}", DiagnosticCode.E_LIMIT)
                break
            }
            val start = index
            val char = text[index]
            when {
                char.isIdentifierStart() -> {
                    index++
                    while (index < text.length && text[index].isIdentifierPart()) index++
                    val word = text.substring(start, index)
                    val kind = KEYWORDS[word] ?: TokenKind.IDENTIFIER
                    addToken(Token(kind, SourceSpan(start, index), word))
                }

                char.isAsciiDigit() -> lexNumber()
                char == '"' -> lexString()
                else -> lexPunctuation()
            }
        }

        tokens.add(Token(TokenKind.EOF, SourceSpan.at(text.length), ""))
        return LexResult(tokens, diagnostics)
    }

    private fun skipWhitespaceAndComments() {
        while (index < text.length) {
            val char = text[index]
            if (char == '\uFEFF' || char.isWhitespace()) {
                index++
                continue
            }
            if (char == '-' && index + 1 < text.length && text[index + 1] == '-') {
                index += 2
                while (index < text.length && text[index] != '\n' && text[index] != '\r') index++
                continue
            }
            break
        }
    }

    private fun lexNumber() {
        val start = index
        while (index < text.length && text[index].isAsciiDigit()) index++
        var isDouble = false

        if (index < text.length && text[index] == '.') {
            isDouble = true
            index++
            if (index >= text.length || !text[index].isAsciiDigit()) {
                addError(SourceSpan(start, index), "a decimal point must be followed by at least one digit")
            } else {
                while (index < text.length && text[index].isAsciiDigit()) index++
            }
        }

        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            isDouble = true
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            if (index >= text.length || !text[index].isAsciiDigit()) {
                addError(SourceSpan(start, index), "an exponent must contain at least one digit")
            } else {
                while (index < text.length && text[index].isAsciiDigit()) index++
            }
        }

        var kind = if (isDouble) TokenKind.DOUBLE else TokenKind.INTEGER
        if (index < text.length && text[index] == 'L') {
            if (isDouble) {
                addError(SourceSpan(index, index + 1), "the Long suffix 'L' is not valid on a Double literal")
                index++
            } else {
                index++
                kind = TokenKind.LONG
            }
        }

        addToken(Token(kind, SourceSpan(start, index), text.substring(start, index)))
    }

    private fun lexString() {
        val start = index
        index++ // opening quote
        val decoded = StringBuilder()
        var terminated = false
        var tooLong = false

        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' -> {
                    index++
                    terminated = true
                }

                char == '\\' -> {
                    val escapeStart = index
                    index++
                    if (index >= text.length) break
                    val escape = text[index]
                    index++
                    when (escape) {
                        '"' -> decoded.append('"')
                        '\\' -> decoded.append('\\')
                        '/' -> decoded.append('/')
                        'b' -> decoded.append('\b')
                        'f' -> decoded.append('\u000C')
                        'n' -> decoded.append('\n')
                        'r' -> decoded.append('\r')
                        't' -> decoded.append('\t')
                        'u' -> {
                            var value = 0
                            var valid = true
                            var consumed = 0
                            while (consumed < 4) {
                                if (index >= text.length || !text[index].isHexDigit()) {
                                    valid = false
                                    break
                                }
                                value = value * 16 + text[index].hexValue()
                                index++
                                consumed++
                            }
                            if (!valid) {
                                addError(
                                    SourceSpan(escapeStart, index),
                                    "invalid Unicode escape: expected four hexadecimal digits",
                                )
                            } else {
                                handleUnicodeUnit(value, escapeStart, decoded)
                            }
                        }

                        else -> {
                            addError(
                                SourceSpan(escapeStart, index),
                                "invalid string escape '\\$escape'",
                            )
                        }
                    }
                    if (decoded.length > limits.maxStringLength && !tooLong) {
                        tooLong = true
                        addError(
                            SourceSpan(start, index),
                            "decoded string exceeds the limit ${limits.maxStringLength} UTF-16 units",
                            DiagnosticCode.E_LIMIT,
                        )
                    }
                }

                char == '\n' || char == '\r' -> break

                char < ' ' -> {
                    addError(
                        SourceSpan(index, index + 1),
                        "unescaped control character U+${char.code.toString(16).uppercase().padStart(4, '0')} in string",
                    )
                    index++
                }

                else -> {
                    decoded.append(char)
                    index++
                    if (decoded.length > limits.maxStringLength && !tooLong) {
                        tooLong = true
                        addError(
                            SourceSpan(start, index),
                            "decoded string exceeds the limit ${limits.maxStringLength} UTF-16 units",
                            DiagnosticCode.E_LIMIT,
                        )
                    }
                }
            }
            if (terminated) break
        }

        if (!terminated) {
            addError(SourceSpan(start, index), "unterminated string literal")
        }
        addToken(Token(TokenKind.STRING, SourceSpan(start, index), text.substring(start, index), decoded.toString()))
    }

    private fun handleUnicodeUnit(value: Int, escapeStart: Int, decoded: StringBuilder) {
        when {
            value in 0xD800..0xDBFF -> {
                // A high surrogate must be followed by an escaped low surrogate; otherwise the source
                // would not denote Unicode scalar values.
                val pairReady = index + 6 <= text.length &&
                    text[index] == '\\' &&
                    text[index + 1] == 'u' &&
                    (2 until 6).all { text[index + it].isHexDigit() }
                if (!pairReady) {
                    addError(SourceSpan(escapeStart, index), "unpaired high surrogate in Unicode escape")
                    decoded.append(value.toChar())
                    return
                }
                var low = 0
                for (offset in 2 until 6) {
                    low = low * 16 + text[index + offset].hexValue()
                }
                if (low !in 0xDC00..0xDFFF) {
                    addError(SourceSpan(escapeStart, index + 6), "high surrogate is not followed by a low surrogate")
                } else {
                    decoded.append(value.toChar())
                    decoded.append(low.toChar())
                }
                index += 6
            }

            value in 0xDC00..0xDFFF -> {
                addError(SourceSpan(escapeStart, index), "unpaired low surrogate in Unicode escape")
                decoded.append(value.toChar())
            }

            else -> decoded.append(value.toChar())
        }
    }

    private fun lexPunctuation() {
        val start = index
        val char = text[index]
        fun single(kind: TokenKind) {
            index++
            addToken(Token(kind, SourceSpan(start, index), text.substring(start, index)))
        }

        when (char) {
            ':' -> single(TokenKind.COLON)
            ';' -> single(TokenKind.SEMICOLON)
            ',' -> single(TokenKind.COMMA)
            '(' -> single(TokenKind.LPAREN)
            ')' -> single(TokenKind.RPAREN)
            '[' -> single(TokenKind.LBRACKET)
            ']' -> single(TokenKind.RBRACKET)
            '+' -> {
                if (index + 1 < text.length && text[index + 1] == '+') {
                    index += 2
                    addToken(Token(TokenKind.PLUSPLUS, SourceSpan(start, index), "++"))
                } else {
                    single(TokenKind.PLUS)
                }
            }

            '-' -> {
                if (index + 1 < text.length && text[index + 1] == '>') {
                    index += 2
                    addToken(Token(TokenKind.ARROW, SourceSpan(start, index), "->"))
                } else {
                    single(TokenKind.MINUS)
                }
            }
            '*' -> single(TokenKind.STAR)
            '/' -> single(TokenKind.SLASH)
            '%' -> single(TokenKind.PERCENT)
            '=' -> {
                if (index + 1 < text.length && text[index + 1] == '=') {
                    index += 2
                    addToken(Token(TokenKind.EQUAL_EQUAL, SourceSpan(start, index), "=="))
                } else {
                    single(TokenKind.EQUALS)
                }
            }

            '!' -> {
                if (index + 1 < text.length && text[index + 1] == '=') {
                    index += 2
                    addToken(Token(TokenKind.NOT_EQUAL, SourceSpan(start, index), "!="))
                } else {
                    index++
                    addError(SourceSpan(start, index), "unexpected character '!' (use '!=' for inequality)")
                }
            }

            '<' -> {
                if (index + 1 < text.length && text[index + 1] == '=') {
                    index += 2
                    addToken(Token(TokenKind.LESS_EQUAL, SourceSpan(start, index), "<="))
                } else {
                    single(TokenKind.LESS)
                }
            }

            '>' -> {
                when {
                    index + 1 < text.length && text[index + 1] == '>' -> {
                        index += 2
                        addToken(Token(TokenKind.GREATER_GREATER, SourceSpan(start, index), ">>"))
                    }

                    index + 1 < text.length && text[index + 1] == '=' -> {
                        index += 2
                        addToken(Token(TokenKind.GREATER_EQUAL, SourceSpan(start, index), ">="))
                    }

                    else -> single(TokenKind.GREATER)
                }
            }

            '&' -> {
                if (index + 1 < text.length && text[index + 1] == '&') {
                    index += 2
                    addToken(Token(TokenKind.AND_AND, SourceSpan(start, index), "&&"))
                } else {
                    index++
                    addError(SourceSpan(start, index), "unexpected character '&' (use '&&' for conjunction)")
                }
            }

            '|' -> {
                if (index + 1 < text.length && text[index + 1] == '|') {
                    index += 2
                    addToken(Token(TokenKind.OR_OR, SourceSpan(start, index), "||"))
                } else {
                    index++
                    addError(SourceSpan(start, index), "unexpected character '|' (use '||' for disjunction)")
                }
            }

            else -> {
                index++
                addError(
                    SourceSpan(start, index),
                    "unexpected character '${char}' (U+${char.code.toString(16).uppercase().padStart(4, '0')})",
                )
            }
        }
    }

    private fun addToken(token: Token) {
        tokens.add(token)
    }

    private fun addError(span: SourceSpan, message: String, code: DiagnosticCode = DiagnosticCode.E_SYNTAX) {
        if (diagnostics.size >= limits.maxDiagnostics) return
        diagnostics.add(Diagnostic.error(code, span, message))
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private fun Char.isHexDigit(): Boolean = isAsciiDigit() || this in 'a'..'f' || this in 'A'..'F'

    private fun Char.hexValue(): Int = when (this) {
        in '0'..'9' -> code - '0'.code
        in 'a'..'f' -> code - 'a'.code + 10
        else -> code - 'A'.code + 10
    }

    private fun Char.isIdentifierStart(): Boolean = this == '_' || this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isIdentifierPart(): Boolean = isIdentifierStart() || isAsciiDigit()

    companion object {
        private val KEYWORDS: Map<String, TokenKind> = mapOf(
            "input" to TokenKind.INPUT,
            "let" to TokenKind.LET,
            "output" to TokenKind.OUTPUT,
            "if" to TokenKind.IF,
            "then" to TokenKind.THEN,
            "else" to TokenKind.ELSE,
            "true" to TokenKind.TRUE,
            "false" to TokenKind.FALSE,
            "op" to TokenKind.OP,
            "item" to TokenKind.ITEM,
            "block" to TokenKind.BLOCK,
            "fluid" to TokenKind.FLUID,
            "nbt" to TokenKind.NBT,
            "Fn" to TokenKind.FN,
        )
    }
}
