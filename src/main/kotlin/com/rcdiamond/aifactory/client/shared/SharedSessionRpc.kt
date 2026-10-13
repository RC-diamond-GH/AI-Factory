package com.rcdiamond.aifactory.client.shared

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import net.minecraft.client.Minecraft
import java.util.UUID

/** Expected, reportable failure of one shared-session exchange; never quotes a raw protocol body. */
class SharedSessionException(message: String) : Exception(message)

/**
 * Client half of the shared-session request/reply channel.
 *
 * Every request carries a fresh UUID and the matching [SharedSessionResponseEvent] publishes that UUID back
 * on the client main thread, so one exchange never completes another. Requests are sent from the client
 * thread and the caller only suspends, so the game thread is never blocked; a request that the server never
 * answers fails after [DEFAULT_TIMEOUT_MILLIS] instead of waiting forever. A disconnect fails everything
 * still pending, and a reply that arrives after its timeout is ignored.
 */
internal object SharedSessionRpc {
    private const val DEFAULT_TIMEOUT_MILLIS = 30_000L

    /** Saves and opens carry a whole conversation, so they are allowed to take longer. */
    const val BULK_TIMEOUT_MILLIS = 120_000L

    private val LOGGER = LogUtils.getLogger()
    private val pending = HashMap<UUID, CompletableDeferred<JsonObject>>()

    /** Delivers the reply of [requestId]; unknown or already timed-out requests are dropped. */
    fun onResponse(requestId: UUID, payload: JsonObject) {
        pending.remove(requestId)?.complete(payload)
    }

    /**
     * Sends one request and suspends until its reply arrives, the server rejects nothing and no local reply
     * matches. Fails with [SharedSessionException] on a timeout, a transport failure or a disconnect.
     */
    suspend fun call(
        operation: String,
        payload: JsonObject,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): JsonObject {
        check(Minecraft.getInstance().isSameThread) { "Shared-session requests must run on the client thread" }
        val requestId = UUID.randomUUID()
        val reply = CompletableDeferred<JsonObject>()
        pending[requestId] = reply
        try {
            SharedSessionNetwork.request(requestId, operation, payload)
        } catch (failure: RuntimeException) {
            pending.remove(requestId)
            reply.completeExceptionally(SharedSessionException("The client could not send the shared-session request"))
            LOGGER.warn("Shared-session request '{}' was not sent", operation)
            throw SharedSessionException("The client could not send the shared-session request")
        }
        val response = try {
            withTimeoutOrNull(timeoutMillis) { reply.await() }
        } finally {
            pending.remove(requestId)
        }
        return response ?: throw SharedSessionException("The server did not answer the '$operation' request in time")
    }

    fun disconnect() {
        SharedSessionNetwork.clearClientAssemblies()
        val abandoned = pending.entries.toList()
        pending.clear()
        for ((requestId, reply) in abandoned) {
            reply.completeExceptionally(SharedSessionException("Disconnected before the server answered"))
            LOGGER.debug("Abandoned the shared-session request {}", requestId)
        }
    }
}
