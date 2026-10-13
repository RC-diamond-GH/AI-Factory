package com.rcdiamond.aifactory.client.game

import com.rcdiamond.aifactory.client.llm.ExecuteCommandGateway
import com.rcdiamond.aifactory.client.llm.ExecuteCommandRequest
import com.rcdiamond.aifactory.client.llm.ExecuteCommandResult
import com.rcdiamond.aifactory.client.llm.GiveItemGateway
import com.rcdiamond.aifactory.client.llm.GiveItemRequest
import com.rcdiamond.aifactory.client.llm.GiveItemResult
import com.rcdiamond.aifactory.network.GameActionNetwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import net.minecraft.client.Minecraft
import java.util.UUID

/** Confined to the Minecraft client thread, including packet replies and cancellation. */
object ClientGameActions : GiveItemGateway, ExecuteCommandGateway {
    private val pending = HashMap<UUID, CompletableDeferred<GiveItemResult>>()
    private val pendingCommands = HashMap<UUID, CompletableDeferred<ExecuteCommandResult>>()

    override suspend fun giveItem(itemId: String, count: Int): GiveItemResult {
        check(Minecraft.getInstance().isSameThread) { "Game actions must run on the client thread" }
        val requestId = UUID.randomUUID()
        val connection = Minecraft.getInstance().connection?.connection
        if (connection == null || !GameActionNetwork.isAvailable(connection)) {
            return GiveItemResult(requestId, false, "The server does not support AI-Factory game actions.")
        }
        val reply = CompletableDeferred<GiveItemResult>()
        pending[requestId] = reply
        try {
            GameActionNetwork.sendGiveItem(GiveItemRequest(requestId, itemId, count))
            return reply.await()
        } finally {
            pending.remove(requestId)
            reply.cancel()
        }
    }

    fun complete(result: GiveItemResult) {
        pending[result.requestId]?.complete(result)
    }

    override suspend fun executeCommand(command: String): ExecuteCommandResult {
        check(Minecraft.getInstance().isSameThread) { "Game actions must run on the client thread" }
        val requestId = UUID.randomUUID()
        val connection = Minecraft.getInstance().connection?.connection
        if (connection == null || !GameActionNetwork.isAvailable(connection)) {
            return ExecuteCommandResult(requestId, false, 0, "The server does not support AI-Factory game actions.")
        }
        val reply = CompletableDeferred<ExecuteCommandResult>()
        pendingCommands[requestId] = reply
        try {
            GameActionNetwork.sendExecuteCommand(ExecuteCommandRequest(requestId, command))
            return reply.await()
        } finally {
            pendingCommands.remove(requestId)
            reply.cancel()
        }
    }

    fun complete(result: ExecuteCommandResult) {
        pendingCommands[result.requestId]?.complete(result)
    }

    fun disconnect() {
        while (pending.isNotEmpty()) {
            val entry = pending.entries.iterator().next()
            pending.remove(entry.key)
            entry.value.cancel(CancellationException("Disconnected from server"))
        }
        while (pendingCommands.isNotEmpty()) {
            val entry = pendingCommands.entries.iterator().next()
            pendingCommands.remove(entry.key)
            entry.value.cancel(CancellationException("Disconnected from server"))
        }
    }
}
