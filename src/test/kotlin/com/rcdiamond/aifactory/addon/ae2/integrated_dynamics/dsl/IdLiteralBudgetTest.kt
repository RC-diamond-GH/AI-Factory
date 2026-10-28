package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdLiteralBudgetTest {
    @Test
    fun `two full static lists require two cards rather than one card per element`() {
        val source = listOf(
            "output first : [Integer] = ${literal(0, 256)};",
            "output second : [Integer] = ${literal(1, 256)};",
        ).joinToString("\n")
        val result = IdDslCompiler.compile(source, IdInputDescription(emptyList()))
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(2, result.cardsRequired)
        val nodes = assertNotNull(result.typedPlan).nodes
        assertEquals(2, nodes.size)
        for (node in nodes) assertEquals(256, ((node as IdPlanNode.Value).value as JsonArray).size)
    }

    @Test
    fun `an existing literal can be reused when distinct values exactly fill the budget`() {
        val source = buildString {
            repeat(15) { append("output xs$it : [Integer] = ${literal(it, 256)};\n") }
            append("output last : [Integer] = ${literal(20, 240)};\n")
            append("output duplicate : [Integer] = ${literal(0, 256)};\n")
        }
        val result = IdDslCompiler.compile(source, IdInputDescription(emptyList()))
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(16, result.cardsRequired)
        assertEquals(17, result.outputCount)
        val outputs = assertNotNull(result.typedPlan).outputs
        assertEquals(
            outputs.single { it.name == "xs0" }.reference,
            outputs.single { it.name == "duplicate" }.reference,
        )
    }

    private fun literal(group: Int, length: Int): String =
        (0 until length).joinToString(",", prefix = "[", postfix = "]") { (group * 1000 + it).toString() }
}
