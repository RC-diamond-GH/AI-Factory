package com.rcdiamond.aifactory.client.llm

import java.util.UUID

/** Configuration data shared by the composition root and provider-independent agent. */
data class ModelSettings(val baseUrl: String, val apiKey: String, val model: String)

data class AgentSettings(
    val systemPrompt: String,
    val maxSteps: Int = 8,
    val requestTimeoutSeconds: Long = 120,
    val toolTimeoutSeconds: Long = 15,
)

sealed interface AgentUpdate {
    data class TextDelta(val text: String) : AgentUpdate
    data class ToolStarted(val name: String) : AgentUpdate
    data class ToolFinished(val name: String, val result: String) : AgentUpdate
}

/** Presentation data for the in-game chat; not a provider-specific response. */
data class ChatEntry(val speaker: String, val text: String)

/** Game UI depends on this contract, never on an Agent or model provider implementation. */
interface AgentChatSession {
    val entries: List<ChatEntry>
    val revision: Long
    val busy: Boolean
    val status: String
    fun submit(message: String)
    fun reload()
    fun cancelTurn()
    fun clear()
}

/** Game-action messages contain no provider-specific types or credentials. */
data class GiveItemRequest(val requestId: UUID, val itemId: String, val count: Int)
data class GiveItemResult(val requestId: UUID, val success: Boolean, val message: String)

/** Client Tool policy depends on this mechanism, not on Minecraft or a network implementation. */
fun interface GiveItemGateway {
    suspend fun giveItem(itemId: String, count: Int): GiveItemResult
}

data class ExecuteCommandRequest(val requestId: UUID, val command: String)
data class ExecuteCommandResult(
    val requestId: UUID,
    val success: Boolean,
    val resultCode: Int,
    val message: String,
    val outputTruncated: Boolean = false,
)

fun interface ExecuteCommandGateway {
    suspend fun executeCommand(command: String): ExecuteCommandResult
}
