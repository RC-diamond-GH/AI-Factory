package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun variableImport(
    name: String,
    type: IdDslType,
    finiteness: IdFiniteness = IdFiniteness.UNKNOWN,
): IdPlanImport = IdPlanImport(name, IdBinding.Variable(1), type, finiteness)

private fun valueOnlyPlan(type: IdDslType, value: kotlinx.serialization.json.JsonElement): IdPlan =
    IdPlan(
        imports = emptyList(),
        nodes = listOf(IdPlanNode.Value(0, type, value)),
        outputs = listOf(IdPlanOutput("result", IdReference.Node(0), type)),
    )

private fun IdPlanValidation.codes(): List<IdDiagnosticCode> = diagnostics.map { it.code }

private fun IdPlanValidation.assertHas(code: IdDiagnosticCode) {
    assertTrue(codes().contains(code), "missing $code in ${diagnostics.joinToString()}")
}

private fun IdPlanValidation.assertOk() {
    assertTrue(ok, diagnostics.joinToString())
}

class IdPlanValidatorTest {

    @Test
    fun `variable-only output is valid and needs no definitions`() {
        val plan = IdPlan(
            imports = listOf(variableImport("x", IdDslTypes.INTEGER)),
            nodes = emptyList(),
            outputs = listOf(IdPlanOutput("result", IdReference.Input("x"), IdDslTypes.INTEGER)),
        )
        val resolved = mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN))
        val validation = IdPlanValidator.validate(plan, resolved)
        validation.assertOk()
        assertEquals(0, validation.plan!!.nodes.size)
    }

    @Test
    fun `imports must be resolved with exactly matching type and finiteness`() {
        val plan = IdPlan(
            imports = listOf(variableImport("x", IdDslTypes.INTEGER)),
            nodes = emptyList(),
            outputs = listOf(IdPlanOutput("result", IdReference.Input("x"), IdDslTypes.INTEGER)),
        )
        val missing = IdPlanValidator.validate(plan, emptyMap())
        assertFalse(missing.ok)
        missing.assertHas(IdDiagnosticCode.E_INPUT_BINDING)

        val mismatch = IdPlanValidator.validate(
            plan,
            mapOf("x" to IdResolvedInput(IdDslTypes.LONG, IdFiniteness.UNKNOWN)),
        )
        assertFalse(mismatch.ok)
        mismatch.assertHas(IdDiagnosticCode.E_TYPE)

        val finiteMismatch = IdPlanValidator.validate(
            plan,
            mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.FINITE)),
        )
        assertFalse(finiteMismatch.ok)
        finiteMismatch.assertHas(IdDiagnosticCode.E_INPUT_BINDING)
    }

    @Test
    fun `call signatures are inferred independently from operator catalog`() {
        val valid = IdPlan(
            imports = listOf(variableImport("x", IdDslTypes.INTEGER)),
            nodes = listOf(
                IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1)),
                IdPlanNode.Call(
                    1,
                    IdDslTypes.INTEGER,
                    IdOperatorCatalog.ARITHMETIC_ADDITION,
                    listOf(IdReference.Input("x"), IdReference.Node(0)),
                ),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(1), IdDslTypes.INTEGER)),
        )
        IdPlanValidator.validate(valid, mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN))).assertOk()

        val wrongResult = valid.copy(
            nodes = valid.nodes.dropLast(1) + (valid.nodes.last() as IdPlanNode.Call).copy(type = IdDslTypes.BOOLEAN),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(1), IdDslTypes.BOOLEAN)),
        )
        IdPlanValidator.validate(wrongResult, mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN)))
            .also {
                assertFalse(it.ok)
                it.assertHas(IdDiagnosticCode.E_FN_SIGNATURE)
            }

        val wrongArgument = valid.copy(
            nodes = listOf(
                IdPlanNode.Value(0, IdDslTypes.LONG, JsonPrimitive(1L)),
                (valid.nodes.last() as IdPlanNode.Call).copy(arguments = listOf(IdReference.Input("x"), IdReference.Node(0))),
            ),
        )
        IdPlanValidator.validate(wrongArgument, mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN)))
            .assertHas(IdDiagnosticCode.E_FN_SIGNATURE)

        val unknown = valid.copy(
            nodes = listOf(
                IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1)),
                (valid.nodes.last() as IdPlanNode.Call).copy(operator = "example:missing"),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(1), IdDslTypes.INTEGER)),
        )
        IdPlanValidator.validate(unknown, mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN)))
            .assertHas(IdDiagnosticCode.E_NATIVE_CONTRACT)
    }

    @Test
    fun `native operator constants carry concrete operator types`() {
        val wrong = IdPlan(
            imports = emptyList(),
            nodes = listOf(IdPlanNode.Native(0, IdDslTypes.BOOLEAN, IdOperatorCatalog.RELATIONAL_EQUALS)),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.BOOLEAN)),
        )
        IdPlanValidator.validate(wrong, emptyMap()).assertHas(IdDiagnosticCode.E_FN_SIGNATURE)
    }

    @Test
    fun `operator apply supports one-step curves and apply0`() {
        val partial = IdPlan(
            imports = emptyList(),
            nodes = listOf(
                IdPlanNode.Native(
                    0,
                    IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.INTEGER),
                    IdOperatorCatalog.ARITHMETIC_ADDITION,
                ),
                IdPlanNode.Value(1, IdDslTypes.INTEGER, JsonPrimitive(1)),
                IdPlanNode.Call(
                    2,
                    IdDslTypes.fn(IdDslTypes.INTEGER, result = IdDslTypes.INTEGER),
                    IdOperatorCatalog.OPERATOR_APPLY,
                    listOf(IdReference.Node(0), IdReference.Node(1)),
                ),
            ),
            outputs = listOf(
                IdPlanOutput(
                    "result",
                    IdReference.Node(2),
                    IdDslTypes.fn(IdDslTypes.INTEGER, result = IdDslTypes.INTEGER),
                ),
            ),
        )
        IdPlanValidator.validate(partial, emptyMap()).assertOk()

        val wrongCurve = partial.copy(
            nodes = partial.nodes.dropLast(1) + (partial.nodes.last() as IdPlanNode.Call).copy(type = IdDslTypes.INTEGER),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(2), IdDslTypes.INTEGER)),
        )
        IdPlanValidator.validate(wrongCurve, emptyMap()).assertHas(IdDiagnosticCode.E_FN_SIGNATURE)

        val zeroArity = IdPlan(
            imports = listOf(variableImport("f", IdDslTypes.fn(result = IdDslTypes.INTEGER))),
            nodes = listOf(
                IdPlanNode.Call(
                    0,
                    IdDslTypes.INTEGER,
                    IdOperatorCatalog.OPERATOR_APPLY_0,
                    listOf(IdReference.Input("f")),
                ),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.INTEGER)),
        )
        val resolved = mapOf("f" to IdResolvedInput(IdDslTypes.fn(result = IdDslTypes.INTEGER), IdFiniteness.UNKNOWN))
        IdPlanValidator.validate(zeroArity, resolved).assertOk()
    }

    @Test
    fun `flip validates every function arity above one`() {
        val inner = IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.INTEGER)
        val flipped = IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.INTEGER)
        val plan = IdPlan(
            imports = listOf(variableImport("f", inner)),
            nodes = listOf(
                IdPlanNode.Call(
                    0,
                    flipped,
                    IdOperatorCatalog.OPERATOR_FLIP,
                    listOf(IdReference.Input("f")),
                ),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), flipped)),
        )
        val resolved = mapOf("f" to IdResolvedInput(inner, IdFiniteness.UNKNOWN))
        IdPlanValidator.validate(plan, resolved).assertOk()

        val wrong = plan.copy(
            nodes = listOf(
                IdPlanNode.Call(
                    0,
                    IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.BOOLEAN),
                    IdOperatorCatalog.OPERATOR_FLIP,
                    listOf(IdReference.Input("f")),
                ),
            ),
            outputs = listOf(
                IdPlanOutput(
                    "result",
                    IdReference.Node(0),
                    IdDslTypes.fn(IdDslTypes.INTEGER, IdDslTypes.INTEGER, IdDslTypes.INTEGER, result = IdDslTypes.BOOLEAN),
                ),
            ),
        )
        IdPlanValidator.validate(wrong, resolved).assertHas(IdDiagnosticCode.E_FN_SIGNATURE)
    }

    @Test
    fun `known infinite consumer is rejected by the server validator too`() {
        val list = IdDslTypes.list(IdDslTypes.ITEM)
        val predicate = IdDslTypes.fn(IdDslTypes.ITEM, result = IdDslTypes.BOOLEAN)
        val plan = IdPlan(
            imports = listOf(variableImport("xs", list, IdFiniteness.INFINITE)),
            nodes = listOf(
                IdPlanNode.Native(0, predicate, IdOperatorCatalog.ITEMSTACK_ENCHANTABLE),
                IdPlanNode.Call(
                    1,
                    list,
                    IdOperatorCatalog.OPERATOR_FILTER,
                    listOf(IdReference.Node(0), IdReference.Input("xs")),
                ),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(1), list)),
        )
        val resolved = mapOf("xs" to IdResolvedInput(list, IdFiniteness.INFINITE))
        val validation = IdPlanValidator.validate(plan, resolved)
        assertFalse(validation.ok)
        validation.assertHas(IdDiagnosticCode.E_INFINITE_CONSUMER)

        val unknown = IdPlanValidator.validate(
            plan.copy(imports = listOf(variableImport("xs", list, IdFiniteness.UNKNOWN))),
            mapOf("xs" to IdResolvedInput(list, IdFiniteness.UNKNOWN)),
        )
        assertTrue(unknown.ok, unknown.diagnostics.joinToString())
        unknown.assertHas(IdDiagnosticCode.W_UNKNOWN_FINITENESS)
    }

    @Test
    fun `literal structure is checked against the concrete node type`() {
        val booleanString = IdPlanValidator.validate(
            valueOnlyPlan(IdDslTypes.BOOLEAN, JsonPrimitive("true")),
            emptyMap(),
        )
        booleanString.assertHas(IdDiagnosticCode.E_TYPE)

        val integerFraction = IdPlanValidator.validate(
            valueOnlyPlan(IdDslTypes.INTEGER, JsonPrimitive(1.5)),
            emptyMap(),
        )
        integerFraction.assertHas(IdDiagnosticCode.E_TYPE)

        val badResource = IdPlanValidator.validate(
            valueOnlyPlan(
                IdDslTypes.ITEM,
                buildJsonObject { put("resource", "iron_ingot") },
            ),
            emptyMap(),
        )
        badResource.assertHas(IdDiagnosticCode.E_LITERAL_RESOURCE)

        val zeroFluid = IdPlanValidator.validate(
            valueOnlyPlan(
                IdDslTypes.FLUID,
                buildJsonObject {
                    put("resource", "minecraft:water")
                    put("amount", 0)
                },
            ),
            emptyMap(),
        )
        zeroFluid.assertHas(IdDiagnosticCode.E_LITERAL_RANGE)

        val badSnbt = IdPlanValidator.validate(
            valueOnlyPlan(IdDslTypes.NBT, JsonPrimitive("{Damage:10")),
            emptyMap(),
        )
        badSnbt.assertHas(IdDiagnosticCode.E_LITERAL_NBT)

        val extraField = IdPlanValidator.validate(
            valueOnlyPlan(
                IdDslTypes.ITEM,
                buildJsonObject {
                    put("resource", "minecraft:stone")
                    put("count", 2)
                },
            ),
            emptyMap(),
        )
        extraField.assertHas(IdDiagnosticCode.E_TYPE)
    }

    @Test
    fun `per-list length, global value node and plan byte bounds are enforced`() {
        val tooLong = IdPlanValidator.validate(
            valueOnlyPlan(
                IdDslTypes.list(IdDslTypes.INTEGER),
                JsonArray(List(IdDslLimits.MAX_LIST_LENGTH + 1) { JsonPrimitive(it) }),
            ),
            emptyMap(),
        )
        tooLong.assertHas(IdDiagnosticCode.E_LIMIT)

        val manyLists = JsonArray(
            List(IdDslLimits.MAX_LIST_LENGTH) {
                JsonArray(List(17) { index -> JsonPrimitive(index) })
            },
        )
        val nested = IdPlanValidator.validate(
            valueOnlyPlan(IdDslTypes.list(IdDslTypes.list(IdDslTypes.INTEGER)), manyLists),
            emptyMap(),
        )
        nested.assertHas(IdDiagnosticCode.E_LIMIT)

        val manyStrings = IdPlan(
            imports = emptyList(),
            nodes = List(40) { index ->
                IdPlanNode.Value(index, IdDslTypes.STRING, JsonPrimitive("a".repeat(8_000)))
            },
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.STRING)),
        )
        IdPlanValidator.validate(manyStrings, emptyMap()).assertHas(IdDiagnosticCode.E_LIMIT)
    }

    @Test
    fun `reachability output type and unused imports are checked`() {
        val unreachable = IdPlan(
            imports = emptyList(),
            nodes = listOf(
                IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1)),
                IdPlanNode.Value(1, IdDslTypes.INTEGER, JsonPrimitive(2)),
            ),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.INTEGER)),
        )
        IdPlanValidator.validate(unreachable, emptyMap()).assertHas(IdDiagnosticCode.E_LIMIT)

        val wrongOutputType = IdPlan(
            imports = emptyList(),
            nodes = listOf(IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1))),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.LONG)),
        )
        IdPlanValidator.validate(wrongOutputType, emptyMap()).assertHas(IdDiagnosticCode.E_TYPE)

        val unusedImport = IdPlan(
            imports = listOf(variableImport("x", IdDslTypes.INTEGER)),
            nodes = listOf(IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1))),
            outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.INTEGER)),
        )
        IdPlanValidator.validate(
            unusedImport,
            mapOf("x" to IdResolvedInput(IdDslTypes.INTEGER, IdFiniteness.UNKNOWN)),
        ).assertHas(IdDiagnosticCode.E_INPUT_BINDING)
    }

    @Test
    fun `exact plan schema rejects unknown fields and forward references`() {
        val valid = IdDslWire.encodePlan(
            IdPlan(
                imports = emptyList(),
                nodes = listOf(IdPlanNode.Value(0, IdDslTypes.INTEGER, JsonPrimitive(1))),
                outputs = listOf(IdPlanOutput("result", IdReference.Node(0), IdDslTypes.INTEGER)),
            ),
        )
        val extra = JsonObject(valid.toMutableMap().apply { put("extra", JsonPrimitive(1)) })
        IdPlanValidator.validate(extra, emptyMap()).assertHas(IdDiagnosticCode.E_TYPE)

        val forward = buildJsonObject {
            put("version", 1)
            put("contract", ID_DSL_CONTRACT)
            put("imports", JsonObject(emptyMap()))
            put("nodes", JsonArray(listOf(
                buildJsonObject {
                    put("id", 0)
                    put("kind", "call")
                    put("type", IdDslWire.encodeType(IdDslTypes.INTEGER))
                    put("operator", IdOperatorCatalog.ARITHMETIC_ADDITION)
                    put("args", JsonArray(listOf(JsonObject(mapOf("node" to JsonPrimitive(1))))))
                },
            )))
            put("outputs", buildJsonObject {
                put("result", buildJsonObject {
                    put("ref", JsonObject(mapOf("node" to JsonPrimitive(0))))
                    put("type", IdDslWire.encodeType(IdDslTypes.INTEGER))
                })
            })
        }
        IdPlanValidator.validate(forward, emptyMap()).assertHas(IdDiagnosticCode.E_TYPE)
    }
}
