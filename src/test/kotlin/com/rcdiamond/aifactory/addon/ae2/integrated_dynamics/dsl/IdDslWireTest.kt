package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdDslWireTest {

    @Test
    fun `types round-trip through the exact wire algebra`() {
        val type = IdDslTypes.fn(
            IdDslTypes.list(IdDslTypes.INTEGER),
            IdDslTypes.fn(IdDslTypes.ITEM, result = IdDslTypes.BOOLEAN),
            result = IdDslTypes.NBT,
        )
        val json = IdDslWire.encodeType(type)
        assertEquals(type, IdDslWire.parseType(json))
    }

    @Test
    fun `type objects reject Any, tuples and extra fields`() {
        for (invalid in listOf(
            JsonObject(emptyMap()),
            JsonObject(mapOf("fn" to JsonArray(emptyList<JsonElement>()), "result" to JsonPrimitive("Integer"), "extra" to JsonPrimitive(1))),
            JsonObject(mapOf("list" to JsonPrimitive("Integer"), "extra" to JsonPrimitive(1))),
            JsonArray(emptyList<JsonElement>()),
        )) {
            val failure = assertFailsWith<IdWireException> { IdDslWire.parseType(invalid) }
            assertTrue(failure.code == IdDiagnosticCode.E_TYPE || failure.code == IdDiagnosticCode.E_LIMIT)
        }
    }

    @Test
    fun `description requires exact bindings types and finiteness`() {
        val valid = buildJsonObject {
            put("ok", true)
            put("contract", ID_DSL_CONTRACT)
            put("inputs", buildJsonObject {
                put("stock", buildJsonObject {
                    put("binding", buildJsonObject {
                        put("kind", "aspect")
                        put("part_id", 4)
                        put("aspect", "integrateddynamics:list_inventory_itemstacks")
                    })
                    put("type", buildJsonObject { put("list", "Item") })
                    put("finite", "finite")
                })
            })
        }
        val description = IdDslWire.parseDescription(valid)
        assertEquals(1, description.inputs.size)
        assertEquals(IdDslTypes.list(IdDslTypes.ITEM), description.inputs.single().type)
        assertEquals(IdDslTypes.list(IdDslTypes.ITEM), IdDslWire.parseType(IdDslWire.encodeType(description.inputs.single().type)))

        val extraField = JsonObject(valid.toMutableMap().apply { put("unknown", JsonPrimitive(1)) })
        assertEquals(IdDiagnosticCode.E_TYPE, assertFailsWith<IdWireException> { IdDslWire.parseDescription(extraField) }.code)

        val badFinite = JsonObject(
            mapOf(
                "inputs" to JsonObject(
                    mapOf(
                        "x" to JsonObject(
                            mapOf(
                                "binding" to JsonObject(mapOf("kind" to JsonPrimitive("variable"), "id" to JsonPrimitive(1))),
                                "type" to JsonPrimitive("Integer"),
                                "finite" to JsonPrimitive("sometimes"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(IdDiagnosticCode.E_INPUT_BINDING, assertFailsWith<IdWireException> { IdDslWire.parseDescription(badFinite) }.code)

        val wrongContract = JsonObject(
            valid.toMutableMap().apply { put("contract", JsonPrimitive("id-9.9.9")) },
        )
        assertEquals(IdDiagnosticCode.E_TARGET_CONTRACT, assertFailsWith<IdWireException> { IdDslWire.parseDescription(wrongContract) }.code)
    }

    @Test
    fun `plan round-trips and rejects unknown fields and forward references`() {
        val plan = IdPlan(
            imports = listOf(
                IdPlanImport("x", IdBinding.Variable(7), IdDslTypes.INTEGER, IdFiniteness.UNKNOWN),
            ),
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
        val json = IdDslWire.encodePlan(plan)
        val parsed = IdDslWire.parsePlan(json)
        assertEquals(plan, parsed)

        val extra = JsonObject(json.toMutableMap().apply { put("surprise", JsonPrimitive(1)) })
        assertEquals(IdDiagnosticCode.E_TYPE, assertFailsWith<IdWireException> { IdDslWire.parsePlan(extra) }.code)

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
        assertEquals(IdDiagnosticCode.E_TYPE, assertFailsWith<IdWireException> { IdDslWire.parsePlan(forward) }.code)
    }

    @Test
    fun `diagnostics and source maps encode stable fields`() {
        val diagnostic = IdDiagnostic(
            code = IdDiagnosticCode.E_INFINITE_CONSUMER,
            severity = IdSeverity.ERROR,
            message = "infinite",
            span = IdSourceSpan(3, 9),
            line = 2,
            column = 4,
            hint = "bound it",
            node = 5,
        )
        val json = IdDslWire.encodeDiagnostic(diagnostic)
        assertEquals("E_INFINITE_CONSUMER", json["code"]!!.let { (it as JsonPrimitive).content })
        assertEquals("error", json["severity"]!!.let { (it as JsonPrimitive).content })
        assertEquals(5, json["node"]!!.let { (it as JsonPrimitive).content.toInt() })
        assertEquals(2, json["line"]!!.let { (it as JsonPrimitive).content.toInt() })

        val map = IdSourceMap(
            nodes = mapOf(0 to IdSourceSpan(1, 2)),
            imports = mapOf("x" to IdSourceSpan(3, 4)),
            outputs = mapOf("result" to IdSourceSpan(5, 6)),
        )
        val encoded = IdDslWire.encodeSourceMap(map)
        assertNotNull(encoded["nodes"])
        assertNotNull(encoded["imports"])
        assertNotNull(encoded["outputs"])
    }
}
