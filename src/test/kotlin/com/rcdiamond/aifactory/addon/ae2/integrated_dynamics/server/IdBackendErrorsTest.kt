package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class IdBackendErrorsTest {
    @Test
    fun `a create failure keeps phase code location hint and placed cards`() {
        val error = IdBackendError.create(
            IdDiagnosticCode.E_NATIVE_CONTRACT,
            "native operator mismatch",
            node = 7,
            input = "stock",
            hint = "Recompile",
        )
        error.partialCreated = 2
        error.partialCards = buildJsonArray {
            add(buildJsonObject {
                put("node", 0)
                put("id", 41)
            })
        }

        val json = error.toJson()
        assertEquals(false, json["ok"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("create", json["phase"]?.jsonPrimitive?.content)
        assertEquals("E_NATIVE_CONTRACT", json["code"]?.jsonPrimitive?.content)
        assertEquals(7, json["node"]?.jsonPrimitive?.intOrNull)
        assertEquals("stock", json["input"]?.jsonPrimitive?.content)
        assertEquals("native operator mismatch", json["error"]?.jsonPrimitive?.content)
        assertEquals("Recompile", json["hint"]?.jsonPrimitive?.content)
        assertEquals(2, json["created"]?.jsonPrimitive?.intOrNull)
        assertEquals(1, assertNotNull(json["cards"]).jsonArray.size)
    }

    @Test
    fun `a describe rejection has no invented success fields`() {
        val json = IdBackendError.describe(
            IdDiagnosticCode.E_INPUT_BINDING,
            "variable id 3 is not resolvable on this authorized network",
            input = "stock",
        ).toJson()
        assertEquals("describe", json["phase"]?.jsonPrimitive?.content)
        assertEquals("E_INPUT_BINDING", json["code"]?.jsonPrimitive?.content)
        assertEquals("stock", json["input"]?.jsonPrimitive?.content)
    }
}
