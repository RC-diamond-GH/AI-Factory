package com.rcdiamond.aifactory.lang

/**
 * A half-open range of UTF-16 offsets in [SourceText].
 *
 * Offsets are what Kotlin strings and Swing/Skia editors use, not UTF-8 byte offsets. A source of
 * length `n` can produce spans in `0..n`; [start] is inclusive and [end] is exclusive.
 */
data class SourceSpan(val start: Int, val end: Int) {
    init {
        require(start >= 0) { "span start must be non-negative: $start" }
        require(end >= start) { "span end $end must not precede start $start" }
    }

    val length: Int get() = end - start

    fun union(other: SourceSpan): SourceSpan {
        if (start == end) return other
        if (other.start == other.end) return this
        return SourceSpan(minOf(start, other.start), maxOf(end, other.end))
    }

    companion object {
        /** An empty span, useful for synthetic nodes. */
        val EMPTY: SourceSpan = SourceSpan(0, 0)

        fun at(offset: Int): SourceSpan = SourceSpan(offset, offset)
    }
}

/** One human-facing position. [line] and [column] are one-based; [offset] and [column] count UTF-16 units. */
data class SourcePosition(val offset: Int, val line: Int, val column: Int)

/**
 * UTF-8 encoded length of [text] without allocating a byte array. When the length would exceed
 * [stopAfter], returns `stopAfter + 1` (or [Int.MAX_VALUE] when that cannot be represented) so a
 * caller can reject oversized input without walking the rest of the string.
 *
 * Surrogate pairs count as four bytes. An unpaired surrogate counts as three bytes (therefore it is
 * never under-counted relative to its 16-bit code unit). The result is at most [Int.MAX_VALUE];
 * strings larger than 2 GiB of encoded bytes are reported as [Int.MAX_VALUE].
 */
fun utf8ByteLength(text: String, stopAfter: Int = Int.MAX_VALUE): Int {
    require(stopAfter >= 0) { "stopAfter must be non-negative" }
    var total = 0L
    var index = 0
    while (index < text.length) {
        val char = text[index]
        val code = char.code
        val bytes = when {
            code < 0x80 -> 1
            code < 0x800 -> 2
            code in 0xD800..0xDBFF -> {
                if (index + 1 < text.length && text[index + 1].code in 0xDC00..0xDFFF) {
                    index++ // consume the low surrogate
                    4
                } else {
                    3
                }
            }

            code in 0xDC00..0xDFFF -> 3
            else -> 3
        }
        total += bytes
        if (total > stopAfter) {
            return if (stopAfter == Int.MAX_VALUE) Int.MAX_VALUE else stopAfter + 1
        }
        index++
    }
    return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/**
 * Immutable source file with a precomputed line index.
 *
 * Line breaks are `\n`, `\r\n` and a lone `\r`. Columns count UTF-16 code units from the first
 * character of the line. Tabs count as one unit; no visual tab expansion is attempted.
 */
class SourceText private constructor(
    val name: String,
    val text: String,
    private val lineStarts: IntArray,
) {
    val length: Int get() = text.length

    /**
     * UTF-8 byte length, the unit used by source-size limits. Computed once from [text] without
     * allocating an intermediate byte array.
     */
    val byteLength: Int by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { utf8ByteLength(text) }

    fun positionAt(offset: Int): SourcePosition {
        val clamped = offset.coerceIn(0, text.length)
        var low = 0
        var high = lineStarts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lineStarts[mid] <= clamped) low = mid + 1 else high = mid - 1
        }
        val lineIndex = maxOf(0, high)
        return SourcePosition(clamped, lineIndex + 1, clamped - lineStarts[lineIndex] + 1)
    }

    /** The text of a one-based line, without its line terminator. */
    fun lineText(line: Int): String? {
        if (line !in 1..lineStarts.size) return null
        val start = lineStarts[line - 1]
        var end = if (line < lineStarts.size) lineStarts[line] else text.length
        while (end > start && (text[end - 1] == '\n' || text[end - 1] == '\r')) end--
        return text.substring(start, end)
    }

    fun slice(span: SourceSpan): String {
        val start = span.start.coerceIn(0, text.length)
        val end = span.end.coerceIn(start, text.length)
        return text.substring(start, end)
    }

    /** A stable `name:line:column` rendering used in diagnostics and tests. */
    fun describe(span: SourceSpan): String {
        val start = positionAt(span.start)
        val end = positionAt(span.end)
        val head = "$name:${start.line}:${start.column}"
        return if (end.line == start.line && end.column != start.column) {
            "$head-${end.column}"
        } else if (end.line != start.line) {
            "$head-${end.line}:${end.column}"
        } else {
            head
        }
    }

    companion object {
        fun of(name: String, text: String): SourceText {
            val starts = ArrayList<Int>()
            starts.add(0)
            var index = 0
            while (index < text.length) {
                when (text[index]) {
                    '\r' -> {
                        index++
                        if (index < text.length && text[index] == '\n') index++
                        starts.add(index)
                    }

                    '\n' -> {
                        index++
                        starts.add(index)
                    }

                    else -> index++
                }
            }
            return SourceText(name, text, starts.toIntArray())
        }

        fun of(name: String, text: CharSequence): SourceText = of(name, text.toString())
    }
}
