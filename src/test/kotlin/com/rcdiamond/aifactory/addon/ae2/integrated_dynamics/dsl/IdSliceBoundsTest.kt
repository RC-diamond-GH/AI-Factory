package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdSliceBoundsTest {
    @Test
    fun `invalid literal slices have located errors and no deployable plan`() {
        for (bounds in listOf("(-1) 5", "10 10")) {
            val source = "let naturals = iterate 0 increment;\noutput result : [Integer] = slice naturals $bounds;"
            val result = IdDslCompiler.compile(source, IdInputDescription(emptyList()))
            assertFalse(result.success)
            assertNull(result.plan)
            val error = result.diagnostics.first { it.code == IdDiagnosticCode.E_LITERAL_RANGE }
            assertEquals(2, error.line)
            assertEquals("output result : [Integer] = ".length + 1, error.column)
            assertEquals(source.indexOf("slice"), error.span.start)
        }
    }

    @Test
    fun `valid and potentially valid dynamic bounds remain allowed but impossible ends reject`() {
        val valid = IdDslCompiler.compile(
            "let naturals = iterate 0 increment; output result = slice naturals 0 1;",
            IdInputDescription(emptyList()),
        )
        assertTrue(valid.success, valid.renderDiagnostics())
        val description = IdInputDescription(listOf(
            IdInputPort("start", IdBinding.Variable(1), IdDslTypes.INTEGER, IdFiniteness.FINITE),
        ))
        val prefix = "input start : Integer; let naturals = iterate 0 increment; output result = slice naturals start "
        val dynamic = IdDslCompiler.compile(prefix + "10;", description)
        assertTrue(dynamic.success, dynamic.renderDiagnostics())
        val impossible = IdDslCompiler.compile(prefix + "0;", description)
        assertFalse(impossible.success)
        assertTrue(impossible.diagnostics.any { it.code == IdDiagnosticCode.E_LITERAL_RANGE })
    }

    @Test
    fun `server rejects invalid direct and curried slices without rejecting a valid boundary`() {
        for (curried in listOf(false, true)) {
            for ((from, to) in listOf(-1 to 5, 10 to 10)) {
                val result = IdPlanValidator.validate(plan(from, to, curried), emptyMap())
                assertFalse(result.ok)
                assertTrue(result.diagnostics.any { it.code == IdDiagnosticCode.E_LITERAL_RANGE })
            }
            val valid = IdPlanValidator.validate(plan(0, 1, curried), emptyMap())
            assertTrue(valid.ok, valid.diagnostics.joinToString())
        }
    }

    private fun plan(from: Int, to: Int, curried: Boolean): IdPlan {
        val list = IdDslTypes.list(IdDslTypes.INTEGER)
        val nodes = mutableListOf<IdPlanNode>(
            IdPlanNode.Value(0, list, JsonArray(listOf(JsonPrimitive(0)))),
            IdPlanNode.Value(1, IdDslTypes.INTEGER, JsonPrimitive(from)),
            IdPlanNode.Value(2, IdDslTypes.INTEGER, JsonPrimitive(to)),
        )
        if (curried) {
            nodes += IdPlanNode.Native(3, IdDslTypes.fn(list, IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = list), IdOperatorCatalog.LIST_SLICE)
            nodes += IdPlanNode.Call(4, IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = list), IdOperatorCatalog.OPERATOR_APPLY,
                listOf(IdReference.Node(3), IdReference.Node(0)))
            nodes += IdPlanNode.Call(5, IdDslTypes.fn(IdDslTypes.INTEGER, result = list), IdOperatorCatalog.OPERATOR_APPLY,
                listOf(IdReference.Node(4), IdReference.Node(1)))
            nodes += IdPlanNode.Call(6, list, IdOperatorCatalog.OPERATOR_APPLY,
                listOf(IdReference.Node(5), IdReference.Node(2)))
        } else {
            nodes += IdPlanNode.Call(3, list, IdOperatorCatalog.LIST_SLICE,
                listOf(IdReference.Node(0), IdReference.Node(1), IdReference.Node(2)))
        }
        return IdPlan(
            imports = emptyList(), nodes = nodes,
            outputs = listOf(IdPlanOutput("result", IdReference.Node(nodes.last().id), list)),
        )
    }
}
