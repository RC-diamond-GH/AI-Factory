package com.rcdiamond.aifactory.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommandFeedbackTest {

    @Test
    fun `success with no feedback is still a success`() {
        val feedback = CommandFeedback(CommandLimits.MAX_OUTPUT_LENGTH)
        feedback.onCommandComplete(true)
        assertTrue(feedback.called)
        assertTrue(feedback.succeeded)
        assertEquals("", feedback.output)
        assertFalse(feedback.truncated)
    }

    @Test
    fun `a callback without success is a failure and silence is never success`() {
        val failed = CommandFeedback(CommandLimits.MAX_OUTPUT_LENGTH)
        failed.onCommandComplete(false)
        assertTrue(failed.called)
        assertFalse(failed.succeeded)

        val silent = CommandFeedback(CommandLimits.MAX_OUTPUT_LENGTH)
        assertFalse(silent.called)
        assertFalse(silent.succeeded)
    }

    @Test
    fun `one successful node among failures is enough`() {
        val feedback = CommandFeedback(CommandLimits.MAX_OUTPUT_LENGTH)
        feedback.onCommandComplete(false)
        feedback.onCommandComplete(false)
        feedback.onCommandComplete(true)
        assertTrue(feedback.succeeded)
    }

    @Test
    fun `feedback lines are joined with newlines while they fit`() {
        val feedback = CommandFeedback(10)
        feedback.append("abc")
        feedback.append("de")
        assertEquals("abc\nde", feedback.output)
        assertFalse(feedback.truncated)
    }

    @Test
    fun `empty lines are ignored`() {
        val feedback = CommandFeedback(10)
        feedback.append("")
        feedback.append("a")
        assertEquals("a", feedback.output)
    }

    @Test
    fun `feedback past the limit is cut off and collecting stops`() {
        val feedback = CommandFeedback(10)
        feedback.append("0123456789ABC")
        assertEquals("0123456789", feedback.output)
        assertTrue(feedback.truncated)

        feedback.append("further")
        assertEquals("0123456789", feedback.output)
    }

    @Test
    fun `a line that no longer fits is dropped whole`() {
        val feedback = CommandFeedback(10)
        feedback.append("0123456789")
        feedback.append("x")
        assertEquals("0123456789", feedback.output)
        assertTrue(feedback.truncated)
    }
}
