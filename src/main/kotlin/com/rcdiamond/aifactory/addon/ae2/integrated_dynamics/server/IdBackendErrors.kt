package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One rejected `id_describe_inputs`/`id_create_cards` request.
 *
 * The session transport already has an `{ok:false,error}` shape, but a card creation can fail in
 * clearly separated stages. Keeping the phase, stable diagnostic code, plan node/input and an
 * actionable hint lets the client attach the failure to the generated source without parsing the
 * message text. No stack trace, model input or raw native exception is echoed here.
 */
internal class IdBackendError(
    val code: IdDiagnosticCode,
    val phase: String,
    message: String,
    val node: Int? = null,
    val input: String? = null,
    val hint: String? = null,
) : IllegalArgumentException(message) {

    /** Set only when cards were already placed before a later create failure. */
    var partialCreated: Int? = null

    /** The exact cards placed before a later create failure, never a fabricated success list. */
    var partialCards: JsonArray? = null

    fun toJson(created: Int? = null, cards: JsonArray? = null): JsonObject = buildJsonObject {
        put("ok", false)
        put("phase", phase)
        put("code", code.name)
        node?.let { put("node", it) }
        input?.let { put("input", it) }
        put("error", message ?: "The Integrated Dynamics request was rejected")
        hint?.let { put("hint", it) }
        (created ?: partialCreated)?.let { put("created", it) }
        (cards ?: partialCards)?.let { put("cards", it) }
    }

    companion object {
        fun describe(code: IdDiagnosticCode, message: String, input: String? = null, hint: String? = null) =
            IdBackendError(code, "describe", message, input = input, hint = hint)

        fun validate(code: IdDiagnosticCode, message: String, node: Int? = null, input: String? = null, hint: String? = null) =
            IdBackendError(code, "validate", message, node = node, input = input, hint = hint)

        fun preflight(code: IdDiagnosticCode, message: String, node: Int? = null, input: String? = null, hint: String? = null) =
            IdBackendError(code, "preflight", message, node = node, input = input, hint = hint)

        fun create(code: IdDiagnosticCode, message: String, node: Int? = null, input: String? = null, hint: String? = null) =
            IdBackendError(code, "create", message, node = node, input = input, hint = hint)
    }
}

/** A native type check failed for a plan node; the backend adds the phase and node index. */
internal class IdNativeContractError(
    val code: IdDiagnosticCode,
    message: String,
    val hint: String? = null,
) : IllegalArgumentException(message)
