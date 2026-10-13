package com.rcdiamond.aifactory.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandLimitsTest {

    @Test
    fun `a leading slash is optional and removed at most once`() {
        assertEquals("say hi", CommandLimits.normalize("say hi"))
        assertEquals("say hi", CommandLimits.normalize("/say hi"))
        assertEquals("say hi", CommandLimits.normalize("  /say hi  "))
        assertEquals("say hi", CommandLimits.normalize("/  say hi"))
        assertNull(CommandLimits.normalize("//say hi"))
        assertNull(CommandLimits.normalize("/ /say hi"))
        assertEquals("a/b", CommandLimits.normalize("a/b"))
    }

    @Test
    fun `internal content is preserved instead of being shell-split or requoted`() {
        val nbt = "give @s minecraft:stone{display:{Name:'{\"text\":\"hi\"}'}} 1"
        assertEquals(nbt, CommandLimits.normalize("/$nbt"))
        assertEquals("say a; b", CommandLimits.normalize("say a; b"))
        assertEquals("say  two  spaces", CommandLimits.normalize("  say  two  spaces  "))
        assertEquals(
            "execute as @s at @s rotated ~ 0 run say ~ ~ ~",
            CommandLimits.normalize("execute as @s at @s rotated ~ 0 run say ~ ~ ~"),
        )
    }

    @Test
    fun `the length boundary is measured on the raw text and is inclusive`() {
        val atLimit = "say " + "a".repeat(CommandLimits.MAX_COMMAND_LENGTH - 4)
        assertEquals(atLimit, CommandLimits.normalize(atLimit))
        assertNull(CommandLimits.normalize(atLimit + "a"))

        // Padded past the limit even though trimming would fall back inside it.
        val padded = " " + atLimit
        assertNull(CommandLimits.normalize(padded))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(CommandLimits.normalize(""))
        assertNull(CommandLimits.normalize("   "))
        assertNull(CommandLimits.normalize("\t"))
        assertNull(CommandLimits.normalize("/"))
        assertNull(CommandLimits.normalize("/   "))
    }

    @Test
    fun `line breaks and NUL are rejected`() {
        assertNull(CommandLimits.normalize("say a\nb"))
        assertNull(CommandLimits.normalize("say a\rb"))
        assertNull(CommandLimits.normalize("say a\u0000b"))
        assertNull(CommandLimits.normalize("/say\n/execute"))
        assertNull(CommandLimits.normalize("say safe\rsay unsafe"))
    }
}
