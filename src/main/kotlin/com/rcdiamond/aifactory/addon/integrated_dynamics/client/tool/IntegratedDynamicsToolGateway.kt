package com.rcdiamond.aifactory.addon.integrated_dynamics.client.tool

import kotlinx.serialization.json.JsonObject

/**
 * The client tools' only way to reach a server-side Integrated Dynamics network.
 *
 * The composition root wires this to the session that carries the request, so nothing here knows
 * about sessions, Minecraft or Integrated Dynamics. [execute] receives an operation from
 * [IntegratedDynamicsTools.OPERATIONS] and the already validated arguments, and must return the
 * server's reply object (`{ok:true,...}` or `{ok:false,error:...}`). A missing, closed or unloaded
 * terminal must come back as an `ok:false` object, not as a thrown exception: a throw aborts the
 * agent's whole turn instead of telling the model what happened.
 */
fun interface IntegratedDynamicsToolGateway {
    suspend fun execute(operation: String, arguments: JsonObject): JsonObject
}
