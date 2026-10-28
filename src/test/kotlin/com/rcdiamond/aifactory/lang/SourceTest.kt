package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceTest {
    @Test
    fun `utf8ByteLength counts ascii and multibyte without allocation`() {
        assertEquals(0, utf8ByteLength(""))
        assertEquals(3, utf8ByteLength("abc"))
        assertEquals(2, utf8ByteLength("é"))
        assertEquals(4, utf8ByteLength("😀"))
        assertEquals(7, utf8ByteLength("aé😀"))
    }

    @Test
    fun `unpaired surrogates are never undercounted`() {
        assertEquals(3, utf8ByteLength("\uD800"))
        assertEquals(3, utf8ByteLength("\uDC00"))
        assertEquals(4, utf8ByteLength("\uD83D\uDE00"))
    }

    @Test
    fun `stopAfter returns an exceeded sentinel early`() {
        assertEquals(3, utf8ByteLength("abc", stopAfter = 3))
        assertEquals(4, utf8ByteLength("abcdef", stopAfter = 3))
        assertEquals(3, utf8ByteLength("abcdef", stopAfter = 2))
        assertEquals(Int.MAX_VALUE, utf8ByteLength("abc", stopAfter = Int.MAX_VALUE))
    }

    @Test
    fun `source positions are one based and count utf16 units`() {
        val source = SourceText.of("test", "a\r\nbc")
        assertEquals(SourcePosition(0, 1, 1), source.positionAt(0))
        assertEquals(SourcePosition(1, 1, 2), source.positionAt(1))
        assertEquals(SourcePosition(3, 2, 1), source.positionAt(3))
        assertEquals(SourcePosition(4, 2, 2), source.positionAt(4))
        assertEquals("a", source.lineText(1))
        assertEquals("bc", source.lineText(2))

        val emoji = SourceText.of("emoji", "😀x")
        assertEquals(3, emoji.length)
        assertEquals(SourcePosition(2, 1, 3), emoji.positionAt(2))
        assertEquals(emoji.byteLength, utf8ByteLength(emoji.text))
    }

    @Test
    fun `descriptions are stable`() {
        val source = SourceText.of("file.idsl", "one\ntwo")
        assertEquals("file.idsl:1:1", source.describe(SourceSpan(0, 0)))
        assertEquals("file.idsl:2:1", source.describe(SourceSpan(4, 4)))
        assertEquals("file.idsl:2:1-4", source.describe(SourceSpan(4, 7)))
    }

    @Test
    fun `span union keeps the widest range`() {
        assertEquals(SourceSpan(2, 9), SourceSpan(2, 4).union(SourceSpan(6, 9)))
        assertEquals(SourceSpan(2, 4), SourceSpan(2, 4).union(SourceSpan.at(2)))
        assertEquals(SourceSpan(2, 4), SourceSpan.EMPTY.union(SourceSpan(2, 4)))
    }

    @Test
    fun `diagnostic render includes code and location`() {
        val source = SourceText.of("grammar.idsl", "let a = nope;")
        val diagnostic = Diagnostic.error(DiagnosticCode.E_UNKNOWN_NAME, SourceSpan(8, 12), "unknown name")
        val rendered = diagnostic.render(source)
        assertTrue(rendered.contains("error[E_UNKNOWN_NAME]"))
        assertTrue(rendered.contains("grammar.idsl:1:9"))
        assertTrue(rendered.contains("unknown name"))
    }
}
