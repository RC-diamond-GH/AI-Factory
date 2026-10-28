package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypedGraphTest {
    @Test
    fun `only reachable nodes and used inputs are emitted`() {
        val module = compileTest(
            """
            input unused : Integer;
            input value : Integer;
            let alias = 41;
            let same = alias;
            output result : Integer = same + value;
            """.trimIndent(),
        ).assertSuccess()

        assertEquals(listOf("value"), module.graph.inputs.map { it.name })
        assertEquals(3, module.graph.nodes.size)
        assertEquals(
            listOf(
                IrOperation.Constant::class,
                IrOperation.Input::class,
                IrOperation.Arithmetic::class,
            ),
            module.graph.nodes.map { it.operation::class },
        )
        assertEquals(IrRef.Node(2), module.output("result")?.reference)
    }

    @Test
    fun `let aliases share one node and outputs may point at the same node`() {
        val module = compileTest(
            """
            let value = 1;
            output first : Integer = value;
            output second : Integer = value;
            """.trimIndent(),
        ).assertSuccess()
        assertEquals(1, module.graph.nodes.size)
        val first = module.output("first")!!.reference
        val second = module.output("second")!!.reference
        assertEquals(first, second)
        assertEquals(IrRef.Node(0), first)
    }

    @Test
    fun `full application lowers to nested Apply over one Primitive node`() {
        val module = compileTest("output x : Integer = add 1 2;").assertSuccess()
        val nodes = module.graph.nodes
        assertEquals(5, nodes.size)
        assertTrue(nodes[0].operation is IrOperation.Primitive)
        assertTrue(nodes[1].operation is IrOperation.Constant)
        val firstApply = nodes[2].operation as IrOperation.Apply
        assertEquals(IrRef.Node(0), firstApply.callee)
        assertEquals(IrRef.Node(1), firstApply.argument)
        assertEquals("Fn(Integer) -> Integer", nodes[2].type.display())
        assertTrue(nodes[3].operation is IrOperation.Constant)
        val secondApply = nodes[4].operation as IrOperation.Apply
        assertEquals(IrRef.Node(2), secondApply.callee)
        assertEquals(IrRef.Node(3), secondApply.argument)
        assertEquals(LangTypes.INTEGER, nodes[4].type)
    }

    @Test
    fun `every supported core operation can be lowered`() {
        val module = compileTest(
            """
            input x : Integer;
            let inc = add 1;
            let twice = mul 2;
            output sum : Integer = x + 1;
            output negated : Integer = -x;
            output composed : Fn(Integer) -> Integer = inc >> twice;
            output lists : [Integer] = [1, 2] ++ [3];
            output logic : Boolean = x > 0 && x < 10;
            output chosen : Integer = if x > 0 then x else 0;
            """.trimIndent(),
        ).assertSuccess()

        val operations = module.graph.nodes.map { it.operation }
        assertTrue(operations.any { it is IrOperation.Input })
        assertTrue(operations.any { it is IrOperation.Primitive })
        assertTrue(operations.any { it is IrOperation.Constant })
        assertTrue(operations.any { it is IrOperation.Apply })
        assertTrue(operations.any { it is IrOperation.Arithmetic })
        assertTrue(operations.any { it is IrOperation.Negate })
        assertTrue(operations.any { it is IrOperation.Compose })
        assertTrue(operations.any { it is IrOperation.ListLiteral })
        assertTrue(operations.any { it is IrOperation.Concat })
        assertTrue(operations.any { it is IrOperation.Comparison })
        assertTrue(operations.any { it is IrOperation.LogicalAnd })
        assertTrue(operations.any { it is IrOperation.Choice })
    }

    @Test
    fun `node ids are contiguous and references always go backwards`() {
        val module = compileTest(
            """
            input x : Integer;
            let inc = add x;
            output value : Integer = if x > 0 then inc 1 else x;
            """.trimIndent(),
        ).assertSuccess()
        for ((index, node) in module.graph.nodes.withIndex()) {
            assertEquals(index, node.id)
            for (reference in node.references()) {
                val target = (reference as IrRef.Node).id
                assertTrue(target < node.id, "node ${node.id} references later node $target")
            }
            assertTrue(node.type.isConcrete())
        }
        for (output in module.graph.outputs) {
            assertTrue((output.reference as IrRef.Node).id in module.graph.nodes.indices)
        }
    }

    @Test
    fun `identical source compiles to identical graph nodes`() {
        val source = """
            input a : Integer;
            let value = a + 1;
            output result : Integer = value * 2;
        """.trimIndent()
        val first = compileTest(source).assertSuccess()
        val second = compileTest(source).assertSuccess()
        assertEquals(first.graph.nodes, second.graph.nodes)
        assertEquals(first.graph.inputs, second.graph.inputs)
        assertEquals(first.graph.outputs, second.graph.outputs)
    }

    @Test
    fun `each primitive occurrence is independently instantiated`() {
        val module = compileTest(
            """
            output first : Integer = length [1];
            output second : Integer = length [2];
            """,
        ).assertSuccess()
        val primitives = module.graph.nodes.filter { it.operation is IrOperation.Primitive }
        assertEquals(2, primitives.size)
        assertEquals("Fn([Integer]) -> Integer", primitives[0].type.display())
        assertEquals(primitives[0].type, primitives[1].type)
        assertTrue(primitives[0] !== primitives[1])
    }

    @Test
    fun `typed outputs preserve function and list types`() {
        val module = compileTest(
            """
            let inc = add 1;
            output function : Fn(Integer) -> Integer = inc;
            output empty : [Integer] = [];
            """.trimIndent(),
        ).assertSuccess()
        assertEquals("Fn(Integer) -> Integer", module.output("function")!!.type.display())
        assertEquals("[Integer]", module.output("empty")!!.type.display())
        val functionNode = module.graph.nodes[(module.output("function")!!.reference as IrRef.Node).id]
        assertTrue(functionNode.type is LangType.FunctionType)
    }
}
