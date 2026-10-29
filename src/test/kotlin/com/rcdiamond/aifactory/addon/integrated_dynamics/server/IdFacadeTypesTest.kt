package com.rcdiamond.aifactory.addon.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.integrated_dynamics.dsl.IdDslTypes
import java.lang.reflect.Proxy
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext
import org.cyclops.integrateddynamics.api.item.IVariableFacade
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes
import org.cyclops.integrateddynamics.core.item.OperatorVariableFacade
import org.cyclops.integrateddynamics.core.item.ValueTypeVariableFacade
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IdFacadeTypesTest {
    @Test
    fun `shared native dependency graph is proved linearly instead of exponentially`() {
        val network = Network(chain(32))
        val proof = network.proof()
        assertEquals(IdDslTypes.INTEGER, proof.variable(32).type)
        assertEquals(33, network.reads)
        assertEquals(IdDslTypes.INTEGER, proof.variable(32).type)
        assertEquals(33, network.reads)
    }

    @Test
    fun `a cached shallow proof cannot bypass the reference depth limit`() {
        val network = Network(chain(33))
        val proof = network.proof()
        assertEquals(IdDslTypes.INTEGER, proof.variable(16).type)
        val error = assertFailsWith<IdBackendError> { proof.variable(33, "tooDeep") }
        assertEquals(IdDiagnosticCode.E_INPUT_BINDING, error.code)
        assertEquals("tooDeep", error.input)
    }

    @Test
    fun `a native reference cycle is rejected without reading live variables`() {
        val facades = chain(0)
        facades[1] = addition(1, 1, 0)
        val network = Network(facades)
        val error = assertFailsWith<IdBackendError> { network.proof().variable(1, "cycle") }
        assertEquals(IdDiagnosticCode.E_INPUT_BINDING, error.code)
        assertEquals("cycle", error.input)
        assertEquals(1, network.reads)
    }

    @Test
    fun `a wide acyclic native graph is stopped by the total proof budget`() {
        val facades = HashMap<Int, IVariableFacade>()
        for (id in 0 until 8191) {
            facades[id] = if (id < 4095) addition(id, id * 2 + 1, id * 2 + 2) else integer(id)
        }
        val network = Network(facades)
        val error = assertFailsWith<IdBackendError> { network.proof().variable(0, "tree") }
        assertEquals(IdDiagnosticCode.E_LIMIT, error.code)
        assertEquals("describe", error.phase)
        assertEquals("tree", error.input)
        assertTrue(network.reads <= 4097, "proof must stop before traversing the complete graph")
    }

    private fun chain(depth: Int): MutableMap<Int, IVariableFacade> = HashMap<Int, IVariableFacade>().also { facades ->
        facades[0] = integer(0)
        for (id in 1..depth) facades[id] = addition(id, id - 1, id - 1)
    }

    private fun integer(id: Int): IVariableFacade =
        ValueTypeVariableFacade(id, ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(1))

    private fun addition(id: Int, left: Int, right: Int): IVariableFacade =
        OperatorVariableFacade(id, Operators.ARITHMETIC_ADDITION, intArrayOf(left, right))

    /** Real native facades; the network exposes metadata lookup only, never evaluation or a world. */
    private class Network(facades: Map<Int, IVariableFacade>) {
        var reads = 0
            private set
        private val network = Proxy.newProxyInstance(
            IPartNetwork::class.java.classLoader,
            arrayOf(IPartNetwork::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hasVariableFacade" -> facades.containsKey(args!![0] as Int)
                "getVariableFacade" -> {
                    check(++reads <= 10000) { "Unbounded facade traversal" }
                    facades[args!![0] as Int]
                }
                else -> error("Unexpected network access: ${method.name}")
            }
        } as IPartNetwork

        // Holder lookup is not used by these numeric/operator-only metadata graphs.
        fun proof() = IdFacadeTypes(network, ValueDeseralizationContext(null))
    }
}
