package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IdBlankCardsTest {
    @Test
    fun `a smaller later stack is drained before a partial stack can waste its free slot`() {
        val counts = IntArray(36) { -1 }
        counts[0] = 3
        counts[1] = 2
        counts[2] = 0
        val plan = IdBlankCards.plan(counts, 2)
        assertEquals(mapOf(1 to 2), plan.extractions)
        assertEquals(listOf(1, 2), plan.outputSlots)
    }

    @Test
    fun `capacity agrees with exhaustive extraction choices for small inventories`() {
        for (first in -1..3) for (second in -1..3) for (third in -1..3) {
            val counts = IntArray(36) { -1 }
            counts[0] = first; counts[1] = second; counts[2] = third
            for (needed in 0..3) {
                val feasible = feasible(first, second, third, needed)
                val result = runCatching { IdBlankCards.plan(counts, needed) }
                val state = "[$first,$second,$third], needed=$needed"
                assertEquals(feasible, result.isSuccess, state)
                if (!feasible) {
                    assertEquals("preflight", assertIs<IdBackendError>(result.exceptionOrNull()).phase)
                    continue
                }
                val plan = result.getOrThrow()
                assertEquals(needed, plan.cardCount, state)
                assertEquals(needed, plan.blankCount, state)
                assertEquals(needed, plan.outputSlots.toSet().size, state)
                for ((slot, taken) in plan.extractions) {
                    assertTrue(counts[slot] > 0 && taken in 1..counts[slot], state)
                }
                for (slot in plan.outputSlots) {
                    assertTrue(
                        counts[slot] == 0 || plan.extractions[slot] == counts[slot],
                        "$state would overwrite slot $slot",
                    )
                }
            }
        }
    }

    @Test
    fun `single blanks can be replaced in a full inventory and aliases need no card`() {
        val counts = IntArray(36) { 1 }
        val plan = IdBlankCards.plan(counts, 36)
        assertEquals(36, plan.cardCount)
        assertEquals(36, plan.extractions.size)
        assertEquals(0, IdBlankCards.plan(IntArray(36) { -1 }, 0).cardCount)
    }

    @Test
    fun `insufficient blanks and physical capacity reject before any allocation`() {
        val noBlanks = assertFailsWith<IdBackendError> { IdBlankCards.plan(IntArray(36), 1) }
        assertEquals("preflight", noBlanks.phase)
        assertEquals(IdDiagnosticCode.E_TARGET_CONTRACT, noBlanks.code)
        val tooMany = assertFailsWith<IdBackendError> { IdBlankCards.plan(IntArray(36) { 64 }, 37) }
        assertEquals("preflight", tooMany.phase)
        assertEquals(IdDiagnosticCode.E_TARGET_CONTRACT, tooMany.code)
    }

    /** Independent brute-force oracle: created cards occupy empty or completely drained slots. */
    private fun feasible(first: Int, second: Int, third: Int, needed: Int): Boolean {
        val free = (if (first == 0) 1 else 0) + (if (second == 0) 1 else 0) + (if (third == 0) 1 else 0)
        for (a in 0..maxOf(0, first)) for (b in 0..maxOf(0, second)) for (c in 0..maxOf(0, third)) {
            if (a + b + c != needed) continue
            val drained = (if (first > 0 && a == first) 1 else 0) +
                (if (second > 0 && b == second) 1 else 0) +
                (if (third > 0 && c == third) 1 else 0)
            if (free + drained >= needed) return true
        }
        return false
    }
}
