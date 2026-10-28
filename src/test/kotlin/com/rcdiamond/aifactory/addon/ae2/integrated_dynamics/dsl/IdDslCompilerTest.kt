package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun variable(
    name: String,
    type: IdDslType,
    finiteness: IdFiniteness = IdFiniteness.FINITE,
): IdInputPort = IdInputPort(name, IdBinding.Variable(1), type, finiteness)

private fun aspectInput(
    name: String,
    type: IdDslType,
    aspectId: String = "integrateddynamics:list_inventory_itemstacks",
    finiteness: IdFiniteness = IdFiniteness.FINITE,
): IdInputPort = IdInputPort(name, IdBinding.Aspect(3, aspectId), type, finiteness)

private fun describe(vararg inputs: IdInputPort): IdInputDescription = IdInputDescription(inputs.toList())

private fun IdCompileResult.calls(operator: String): List<IdPlanNode.Call> =
    assertNotNull(typedPlan).nodes.filterIsInstance<IdPlanNode.Call>().filter { it.operator == operator }

private fun IdCompileResult.natives(operator: String): List<IdPlanNode.Native> =
    assertNotNull(typedPlan).nodes.filterIsInstance<IdPlanNode.Native>().filter { it.operator == operator }

private fun IdCompileResult.values(): List<IdPlanNode.Value> =
    assertNotNull(typedPlan).nodes.filterIsInstance<IdPlanNode.Value>()

private fun IdCompileResult.assertCodes(vararg codes: IdDiagnosticCode) {
    for (code in codes) {
        assertTrue(diagnostics.any { it.code == code }, "missing $code in ${diagnostics.joinToString()}")
    }
}

private fun IdCompileResult.validateOwnPlan(): IdPlanValidation {
    val plan = assertNotNull(typedPlan)
    val resolved = plan.imports.associate { it.name to IdResolvedInput(it.type, it.finiteness) }
    return IdPlanValidator.validate(IdDslWire.encodePlan(plan), resolved)
}

class IdDslCompilerTest {

    // ------------------------------------------------------------------ complete examples

    @Test
    fun `restock example compiles to direct native calls and one aspect import`() {
        val source = """
            input stock : [Item];

            let iron = item "minecraft:iron_ingot";
            let limit = 64;
            let count = itemCount stock iron;
            let need = count < limit;

            output request : Item = if need then iron else emptyItem;
            output enabled : Boolean = need;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe(aspectInput("stock", IdDslTypes.list(IdDslTypes.ITEM))))
        assertTrue(result.success, result.renderDiagnostics())
        val plan = assertNotNull(result.typedPlan)

        assertEquals(1, plan.imports.size)
        assertEquals("stock", plan.imports.single().name)
        assertTrue(plan.imports.single().binding is IdBinding.Aspect)
        assertEquals(6, plan.nodes.size)
        assertEquals(7, result.cardsRequired)

        val count = result.calls(IdOperatorCatalog.ITEMSTACK_ITEMLISTCOUNT).single()
        assertEquals(2, count.arguments.size)
        assertTrue(count.arguments[0] is IdReference.Input)
        assertTrue(count.arguments[1] is IdReference.Node)
        assertTrue(result.calls(IdOperatorCatalog.RELATIONAL_LT).single().arguments.size == 2)
        assertTrue(result.calls(IdOperatorCatalog.GENERAL_CHOICE).single().arguments.size == 3)

        assertEquals(IdDslTypes.ITEM, plan.outputs.single { it.name == "request" }.type)
        assertEquals(IdDslTypes.BOOLEAN, plan.outputs.single { it.name == "enabled" }.type)
        assertEquals(IdDslTypes.ITEM, plan.nodes[0].type)
        assertEquals(JsonObject(mapOf("resource" to JsonPrimitive("minecraft:iron_ingot"))), result.values().single { it.id == 0 }.value)
        assertEquals(JsonPrimitive(64), result.values().single { it.id == 2 }.value)
        assertEquals(JsonNull, result.values().single { it.id == 4 }.value)

        assertEquals(1, result.sourceMap!!.imports.size)
        assertEquals(6, result.sourceMap!!.nodes.size)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `function predicate example keeps a function value and does not evaluate it`() {
        val source = """
            input enabled : Boolean;

            let predicate : Fn(Item) -> Boolean =
                if enabled then itemIsEnchantable else constant false;

            output filterOut : Fn(Item) -> Boolean = predicate;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe(variable("enabled", IdDslTypes.BOOLEAN)))
        assertTrue(result.success, result.renderDiagnostics())
        val plan = assertNotNull(result.typedPlan)
        assertEquals(IdDslTypes.fn(IdDslTypes.ITEM, result = IdDslTypes.BOOLEAN), plan.outputs.single().type)
        assertEquals(1, result.calls(IdOperatorCatalog.GENERAL_CHOICE).size)
        assertEquals(1, result.natives(IdOperatorCatalog.ITEMSTACK_ENCHANTABLE).size)
        assertEquals(1, result.natives(IdOperatorCatalog.GENERAL_CONSTANT).size)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `operator pipeline and predicate conjunction compile through native higher-order calls`() {
        val source = """
            input modName : String;

            let fromMod : Fn(Item) -> Boolean =
                itemMod >> eq modName;

            let acceptable : Fn(Item) -> Boolean =
                allOf fromMod itemIsEnchantable;

            output predicate : Fn(Item) -> Boolean = acceptable;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe(variable("modName", IdDslTypes.STRING)))
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(1, result.calls(IdOperatorCatalog.OPERATOR_PIPE).size)
        assertEquals(1, result.calls(IdOperatorCatalog.OPERATOR_CONJUNCTION).size)
        assertEquals(1, result.calls(IdOperatorCatalog.OPERATOR_APPLY).size)
        assertEquals(1, result.natives(IdOperatorCatalog.ITEMSTACK_MOD).size)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `op resolves only pinned catalog entries`() {
        val valid = IdDslCompiler.compile(
            """output mod : Fn(Item) -> String = op "integrateddynamics:itemstack_mod";""",
            describe(),
        )
        assertTrue(valid.success, valid.renderDiagnostics())
        assertEquals(1, valid.natives(IdOperatorCatalog.ITEMSTACK_MOD).size)

        val invalid = IdDslCompiler.compile(
            """output mod : Fn(Item) -> String = op "example:missing";""",
            describe(),
        )
        assertFalse(invalid.success)
        invalid.assertCodes(IdDiagnosticCode.E_NATIVE_CONTRACT)
    }

    @Test
    fun `delayer history program compiles without a graph cycle`() {
        val source = """
            input history : [Boolean];
            input start : Boolean;
            input stop : Boolean;

            let previous = getOr history 0 false;
            output next : Boolean = if stop then false else start || previous;
        """.trimIndent()

        val result = IdDslCompiler.compile(
            source,
            describe(
                variable("history", IdDslTypes.list(IdDslTypes.BOOLEAN)),
                variable("start", IdDslTypes.BOOLEAN),
                variable("stop", IdDslTypes.BOOLEAN),
            ),
        )
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(1, result.calls(IdOperatorCatalog.LIST_GET_OR_DEFAULT).size)
        assertTrue(result.calls(IdOperatorCatalog.LOGICAL_OR).size == 1)
    }

    // ------------------------------------------------------------------ application / laziness

    @Test
    fun `partial application uses a native operator constant and operator_apply`() {
        val source = """
            output inc : Fn(Integer) -> Integer = add 1;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertTrue(result.success, result.renderDiagnostics())
        val plan = assertNotNull(result.typedPlan)
        assertEquals(3, plan.nodes.size)
        val native = result.natives(IdOperatorCatalog.ARITHMETIC_ADDITION).single()
        assertEquals(IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.INTEGER), native.type)
        val apply = result.calls(IdOperatorCatalog.OPERATOR_APPLY).single()
        assertEquals(listOf(IdReference.Node(native.id), IdReference.Node(1)), apply.arguments)
        assertEquals(IdDslTypes.fn(IdDslTypes.INTEGER, result = IdDslTypes.INTEGER), apply.type)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `dynamic function input is applied with operator_apply`() {
        val source = """
            input predicate : Fn(Item) -> Boolean;
            output result : Boolean = predicate (item "minecraft:stone");
        """.trimIndent()

        val result = IdDslCompiler.compile(
            source,
            describe(variable("predicate", IdDslTypes.fn(IdDslTypes.ITEM, result = IdDslTypes.BOOLEAN))),
        )
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(1, result.calls(IdOperatorCatalog.OPERATOR_APPLY).size)
        val apply = result.calls(IdOperatorCatalog.OPERATOR_APPLY).single()
        assertTrue(apply.arguments[0] is IdReference.Input)
        assertTrue(apply.arguments[1] is IdReference.Node)
        assertEquals(IdDslTypes.BOOLEAN, apply.type)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `logical and division are emitted, never constant-folded`() {
        val source = """
            output result : Boolean = false && (1 / 0 == 0);
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(1, result.calls(IdOperatorCatalog.LOGICAL_AND).size)
        assertEquals(1, result.calls(IdOperatorCatalog.ARITHMETIC_DIVISION).size)
        assertEquals(1, result.calls(IdOperatorCatalog.RELATIONAL_EQUALS).size)
        assertEquals(IdDslTypes.BOOLEAN, assertNotNull(result.typedPlan).outputs.single().type)
    }

    // ------------------------------------------------------------------ finiteness

    @Test
    fun `known infinite list passed to filter is a compile error`() {
        val source = """
            let naturals = iterate 0 increment;
            output result : [Integer] = filter (flip lt 10) naturals;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertFalse(result.success)
        assertNull(result.plan)
        assertNull(result.typedPlan)
        result.assertCodes(IdDiagnosticCode.E_INFINITE_CONSUMER)
        assertTrue(result.diagnostics.single { it.code == IdDiagnosticCode.E_INFINITE_CONSUMER }.line > 0)
    }

    @Test
    fun `unknown list finiteness is allowed with a warning`() {
        val source = """
            input xs : [Integer];
            output result : [Integer] = filter (flip lt 10) xs;
        """.trimIndent()

        val result = IdDslCompiler.compile(
            source,
            describe(variable("xs", IdDslTypes.list(IdDslTypes.INTEGER), IdFiniteness.UNKNOWN)),
        )
        assertTrue(result.success, result.renderDiagnostics())
        result.assertCodes(IdDiagnosticCode.W_UNKNOWN_FINITENESS)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `slice with a literal end bounds an infinite list`() {
        val source = """
            let naturals = iterate 0 increment;
            output firstTen : [Integer] = slice naturals 0 10;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(1, result.calls(IdOperatorCatalog.LIST_SLICE).size)
        assertEquals(1, result.calls(IdOperatorCatalog.LIST_LAZYBUILT).size)
        assertTrue(result.validateOwnPlan().ok)
    }

    // ------------------------------------------------------------------ lists / literals

    @Test
    fun `fully static list is one value node with concrete element type`() {
        val source = """
            output items : [Item] = [item "minecraft:iron_ingot", emptyItem];
            output names : [String] = ["a", "b"];
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertTrue(result.success, result.renderDiagnostics())
        val values = result.values()
        assertEquals(2, values.size)
        assertEquals(0, result.calls(IdOperatorCatalog.LIST_APPEND).size)
        val items = values.single { it.type == IdDslTypes.list(IdDslTypes.ITEM) }
        val array = items.value as JsonArray
        assertEquals(2, array.size)
        assertEquals(JsonObject(mapOf("resource" to JsonPrimitive("minecraft:iron_ingot"))), array[0])
        assertEquals(JsonNull, array[1])
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `dynamic list starts concrete empty and appends in order`() {
        val source = """
            input x : Integer;
            output xs : [Integer] = [1, x];
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe(variable("x", IdDslTypes.INTEGER)))
        assertTrue(result.success, result.renderDiagnostics())
        val plan = assertNotNull(result.typedPlan)
        assertEquals(4, plan.nodes.size)
        assertEquals(2, result.calls(IdOperatorCatalog.LIST_APPEND).size)
        val empty = result.values().single { (it.value as? JsonArray)?.isEmpty() == true }
        assertEquals(IdDslTypes.list(IdDslTypes.INTEGER), empty.type)
        assertEquals(0, (empty.value as JsonArray).size)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `object literals use resource JSON and SNBT text`() {
        val source = """
            output blockValue : Block = block "minecraft:stone";
            output fluidValue : Fluid = fluid "minecraft:water" 1000;
            output tag : NBT = nbt "{Damage:10}";
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertTrue(result.success, result.renderDiagnostics())
        val values = result.values()
        assertEquals(JsonObject(mapOf("resource" to JsonPrimitive("minecraft:stone"))), values.single { it.type == IdDslTypes.BLOCK }.value)
        assertEquals(
            JsonObject(mapOf("resource" to JsonPrimitive("minecraft:water"), "amount" to JsonPrimitive(1000))),
            values.single { it.type == IdDslTypes.FLUID }.value,
        )
        assertEquals(JsonPrimitive("{Damage:10}"), values.single { it.type == IdDslTypes.NBT }.value)
        assertTrue(result.validateOwnPlan().ok)
    }

    @Test
    fun `invalid resource and SNBT literals fail before a plan exists`() {
        val resource = IdDslCompiler.compile(
            """output x : Item = item "iron_ingot";""",
            describe(),
        )
        assertFalse(resource.success)
        resource.assertCodes(IdDiagnosticCode.E_LITERAL_RESOURCE)

        val snbt = IdDslCompiler.compile(
            """output x : NBT = nbt "{Damage:10";""",
            describe(),
        )
        assertFalse(snbt.success)
        snbt.assertCodes(IdDiagnosticCode.E_LITERAL_NBT)

        val fluid = IdDslCompiler.compile(
            """output x : Fluid = fluid "minecraft:water" 0;""",
            describe(),
        )
        assertFalse(fluid.success)
        fluid.assertCodes(IdDiagnosticCode.E_LITERAL_RANGE)
    }

    @Test
    fun `let aliases are shared and unreachable lets produce no node`() {
        val source = """
            input x : Integer;
            let shared = x + 1;
            let unused = 99;
            output a : Integer = shared;
            output b : Integer = shared;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe(variable("x", IdDslTypes.INTEGER)))
        assertTrue(result.success, result.renderDiagnostics())
        val plan = assertNotNull(result.typedPlan)
        assertEquals(2, plan.nodes.size)
        assertEquals(1, result.calls(IdOperatorCatalog.ARITHMETIC_ADDITION).size)
        assertTrue(result.values().none { it.value == JsonPrimitive(99) })
        assertEquals(plan.outputs[0].reference, plan.outputs[1].reference)
    }

    // ------------------------------------------------------------------ diagnostics / bindings

    @Test
    fun `mixed numeric types are rejected without implicit conversion`() {
        val result = IdDslCompiler.compile(
            """
                input energy : Long;
                output result : Boolean = energy < 1000;
            """.trimIndent(),
            describe(variable("energy", IdDslTypes.LONG)),
        )
        assertFalse(result.success)
        result.assertCodes(IdDiagnosticCode.E_TYPE)
    }

    @Test
    fun `cycle reports E_CYCLE and no plan`() {
        val result = IdDslCompiler.compile(
            """
                let a = b;
                let b = a;
                output result : Integer = a;
            """.trimIndent(),
            describe(),
        )
        assertFalse(result.success)
        assertNull(result.plan)
        result.assertCodes(IdDiagnosticCode.E_CYCLE)
    }

    @Test
    fun `missing extra and mismatched bindings are all diagnosed`() {
        val missing = IdDslCompiler.compile(
            """input x : Integer; output result : Integer = x;""",
            describe(),
        )
        assertFalse(missing.success)
        missing.assertCodes(IdDiagnosticCode.E_INPUT_BINDING)

        val extra = IdDslCompiler.compile(
            """output result : Integer = 1;""",
            describe(variable("x", IdDslTypes.INTEGER)),
        )
        assertFalse(extra.success)
        extra.assertCodes(IdDiagnosticCode.E_INPUT_BINDING)

        val mismatch = IdDslCompiler.compile(
            """input x : Integer; output result : Integer = x;""",
            describe(variable("x", IdDslTypes.LONG)),
        )
        assertFalse(mismatch.success)
        mismatch.assertCodes(IdDiagnosticCode.E_TYPE)
    }

    @Test
    fun `aspect binding must match the fixed list contract`() {
        val source = """input stock : [Item]; output result : Integer = length stock;"""
        val wrongType = IdDslCompiler.compile(
            source,
            describe(aspectInput("stock", IdDslTypes.list(IdDslTypes.INTEGER))),
        )
        assertFalse(wrongType.success)
        wrongType.assertCodes(IdDiagnosticCode.E_INPUT_BINDING)

        val unknown = IdDslCompiler.compile(
            source,
            describe(aspectInput("stock", IdDslTypes.list(IdDslTypes.ITEM), aspectId = "example:list_custom")),
        )
        assertFalse(unknown.success)
        unknown.assertCodes(IdDiagnosticCode.E_INPUT_BINDING)
    }

    @Test
    fun `unused input still requires a binding and only warns when present`() {
        val source = """input x : Integer; output result : Integer = 1;"""
        val unbound = IdDslCompiler.compile(source, describe())
        assertFalse(unbound.success)
        unbound.assertCodes(IdDiagnosticCode.E_INPUT_BINDING)

        val bound = IdDslCompiler.compile(source, describe(variable("x", IdDslTypes.INTEGER)))
        assertTrue(bound.success, bound.renderDiagnostics())
        bound.assertCodes(IdDiagnosticCode.W_UNUSED_BINDING)
        assertTrue(assertNotNull(bound.typedPlan).imports.isEmpty())
    }

    @Test
    fun `description shape errors are target contract errors and never throw`() {
        val source = """output result : Integer = 1;"""
        val result = IdDslCompiler.compile(source, JsonObject(mapOf("inputs" to JsonPrimitive("nope"))))
        assertFalse(result.success)
        result.assertCodes(IdDiagnosticCode.E_TARGET_CONTRACT)
        assertNull(result.plan)

        val unknownField = IdDslCompiler.compile(
            source,
            JsonObject(mapOf("inputs" to JsonObject(emptyMap()), "surprise" to JsonPrimitive(1))),
        )
        assertFalse(unknownField.success)
        unknownField.assertCodes(IdDiagnosticCode.E_TARGET_CONTRACT)
    }


    // ------------------------------------------------------------------ limits

    @Test
    fun `oversized source is rejected before a line index is built`() {
        val result = IdDslCompiler.compile(
            " ".repeat(IdDslLimits.MAX_SOURCE_BYTES + 1),
            describe(),
        )
        assertFalse(result.success)
        assertNull(result.plan)
        result.assertCodes(IdDiagnosticCode.E_LIMIT)
    }

    @Test
    fun `exponential static list expansion hits the budget before serialization`() {
        val leaves = (0 until 256).joinToString(",")
        val source = """
            let a = [$leaves];
            let b = [a, a];
            let c = [b, b];
            let d = [c, c];
            let e = [d, d];
            output xs : [[[[[Integer]]]]] = e;
        """.trimIndent()

        val result = IdDslCompiler.compile(source, describe())
        assertFalse(result.success)
        assertNull(result.plan)
        result.assertCodes(IdDiagnosticCode.E_LIMIT)
    }

    @Test
    fun `oversized list literal is an E_LIMIT`() {
        val elements = (0..IdDslLimits.MAX_LIST_LENGTH).joinToString(",")
        val result = IdDslCompiler.compile(
            "output xs : [Integer] = [$elements];",
            describe(),
        )
        assertFalse(result.success)
        result.assertCodes(IdDiagnosticCode.E_LIMIT)
    }

    @Test
    fun `two names for one reader aspect require one physical card`() {
        val source = """
            input first : [Item];
            input second : [Item];
            output left = first;
            output right = second;
        """.trimIndent()
        val result = IdDslCompiler.compile(
            source,
            describe(
                aspectInput("first", IdDslTypes.list(IdDslTypes.ITEM)),
                aspectInput("second", IdDslTypes.list(IdDslTypes.ITEM)),
            ),
        )
        assertTrue(result.success, result.renderDiagnostics())
        assertEquals(0, result.nodeCount)
        assertEquals(2, result.outputCount)
        assertEquals(1, result.cardsRequired)
        assertTrue(result.validateOwnPlan().ok)
    }
}
