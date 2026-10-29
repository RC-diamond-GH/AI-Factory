package com.rcdiamond.aifactory.addon.integrated_dynamics.dsl

/**
 * Conservative SNBT syntax check used by the client compiler before a tag literal reaches a plan.
 *
 * The server still parses the tag with the pinned NBT library; this checker only rejects clearly
 * malformed text (unbalanced compounds/lists, unterminated strings, dangling separators). It accepts
 * unquoted keys/values and typed array prefixes so it does not reject valid 1.20.1 SNBT.
 */
object IdSnbt {
    private const val MAX_DEPTH = 32

    /** Returns null when [text] is structurally plausible, otherwise a human-readable reason. */
    fun validate(text: String): String? {
        if (text.isEmpty()) return "SNBT tag text must not be empty"
        return try {
            val scanner = Scanner(text)
            scanner.skipWhitespace()
            scanner.value(0)
            scanner.skipWhitespace()
            if (!scanner.done()) "unexpected trailing text at offset ${scanner.index}" else null
        } catch (failure: SyntaxProblem) {
            failure.message
        }
    }

    private class SyntaxProblem(override val message: String) : RuntimeException(message)

    private class Scanner(private val text: String) {
        var index: Int = 0
            private set

        fun done(): Boolean = index >= text.length

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun value(depth: Int) {
            if (depth > MAX_DEPTH) throw SyntaxProblem("SNBT nesting exceeds $MAX_DEPTH")
            skipWhitespace()
            if (done()) throw SyntaxProblem("unexpected end of SNBT")
            when (text[index]) {
                '{' -> compound(depth + 1)
                '[' -> list(depth + 1)
                '"', '\'' -> quoted()
                else -> token()
            }
        }

        private fun compound(depth: Int) {
            index++ // {
            skipWhitespace()
            if (peek() == '}') {
                index++
                return
            }
            while (true) {
                skipWhitespace()
                if (done()) throw SyntaxProblem("unterminated compound")
                key()
                skipWhitespace()
                if (peek() != ':') throw SyntaxProblem("expected ':' in compound at offset $index")
                index++
                value(depth)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        skipWhitespace()
                        if (peek() == '}') {
                            index++
                            return
                        }
                    }

                    '}' -> {
                        index++
                        return
                    }

                    else -> throw SyntaxProblem("expected ',' or '}' in compound at offset $index")
                }
            }
        }

        private fun list(depth: Int) {
            index++ // [
            skipWhitespace()
            // Typed array prefixes: [B;...], [I;...], [L;...]
            if (index + 1 < text.length && text[index] in "BIL" && text[index + 1] == ';') {
                index += 2
            }
            skipWhitespace()
            if (peek() == ']') {
                index++
                return
            }
            while (true) {
                value(depth)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        skipWhitespace()
                        if (peek() == ']') {
                            index++
                            return
                        }
                    }

                    ']' -> {
                        index++
                        return
                    }

                    else -> throw SyntaxProblem("expected ',' or ']' in list at offset $index")
                }
            }
        }

        private fun key() {
            if (done()) throw SyntaxProblem("unterminated compound key")
            when (text[index]) {
                '"', '\'' -> quoted()
                else -> {
                    val start = index
                    while (index < text.length) {
                        val c = text[index]
                        if (c == ':' || c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c.isWhitespace()) break
                        index++
                    }
                    if (index == start) throw SyntaxProblem("empty compound key at offset $start")
                }
            }
        }

        private fun quoted() {
            val quote = text[index]
            index++
            while (index < text.length) {
                when (text[index]) {
                    '\\' -> {
                        index++
                        if (index >= text.length) throw SyntaxProblem("unterminated escape in string")
                        index++
                    }

                    quote -> {
                        index++
                        return
                    }

                    else -> index++
                }
            }
            throw SyntaxProblem("unterminated quoted string")
        }

        private fun token() {
            val start = index
            while (index < text.length) {
                val c = text[index]
                if (c.isWhitespace() || c == ',' || c == ']' || c == '}' || c == ':' || c == '{' || c == '[') break
                index++
            }
            if (index == start) throw SyntaxProblem("expected a value at offset $start")
        }

        private fun peek(): Char = if (index < text.length) text[index] else '\u0000'
    }
}
