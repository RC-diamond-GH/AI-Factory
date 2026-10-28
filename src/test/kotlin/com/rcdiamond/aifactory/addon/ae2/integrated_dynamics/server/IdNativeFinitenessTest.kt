package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdBinding
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslCompiler
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslTypes
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdFiniteness
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdInputDescription
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdInputPort
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdNativeFinitenessTest {
    @Test
    fun `native identity and constant cannot hide an infinite list from a full consumer`() {
        for (result in compileConsumers(IdFiniteness.INFINITE)) {
            assertFalse(result.success)
            assertTrue(result.plan == null)
            assertTrue(result.diagnostics.any { it.code == IdDiagnosticCode.E_INFINITE_CONSUMER })
        }
    }

    @Test
    fun `finite native identity and constant lists remain valid consumer inputs`() {
        for (result in compileConsumers(IdFiniteness.FINITE)) {
            assertTrue(result.success, result.diagnostics.toString())
            assertNotNull(result.plan)
            assertFalse(result.diagnostics.any { it.code == IdDiagnosticCode.W_UNKNOWN_FINITENESS })
        }
    }

    @Test
    fun `unknown native identity and constant lists retain the consumer warning`() {
        for (result in compileConsumers(IdFiniteness.UNKNOWN)) {
            assertTrue(result.success, result.diagnostics.toString())
            assertNotNull(result.plan)
            assertTrue(result.diagnostics.any { it.code == IdDiagnosticCode.W_UNKNOWN_FINITENESS })
        }
    }

    private fun compileConsumers(finiteness: IdFiniteness) = listOf(
        Operators.GENERAL_IDENTITY to listOf(IdNativeTyped(IdDslTypes.list(IdDslTypes.INTEGER), finiteness)),
        Operators.GENERAL_CONSTANT to listOf(
            IdNativeTyped(IdDslTypes.list(IdDslTypes.INTEGER), finiteness),
            IdNativeTyped(IdDslTypes.BOOLEAN, IdFiniteness.FINITE),
        ),
    ).map { (operator, arguments) ->
        val inferred = IdNativeCatalog.inferCall(operator, arguments)
        val description = IdInputDescription(listOf(IdInputPort("xs", IdBinding.Variable(1), inferred.type, inferred.finite)))
        IdDslCompiler.compile("input xs : [Integer]; output result = reduce add xs 0;", description)
    }
}
