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
    val conversationTitle: String? get() = null
    fun submit(message: String)
    fun reload()
    fun cancelTurn()
    fun clear()

    /**
     * True when closing the chat screen must cancel the running turn, false when the turn has to keep
     * running while nothing is shown (a shared server session). Defaults to the local chat behaviour so
     * existing session implementations stay unchanged.
     */
    val cancelTurnOnClose: Boolean get() = true

    /**
     * Called once when a chat screen for this session becomes visible. A session that owns server state
     * uses this to announce that its screen is open; the local chat ignores it.
     */
    fun onScreenOpened() {}

    /**
     * Called when a chat screen for this session stops being shown, whether the player closed the screen
     * or another screen replaced it. Always runs, also for sessions that cancel on close.
     */
    fun onScreenClosed() {}
}

/** One shared session advertised by the server, without its conversation. */
data class SharedSessionInfo(
    val id: UUID,
    val cellId: UUID,
    val title: String,
    val hash: String,
    val tokens: Long,
    val autoTitle: Boolean = false,
)

/** One intelligent storage cell mounted inside a terminal's grid, as the server mounts it. */
data class SharedCellInfo(val id: UUID, val bytes: Long, val usedBytes: Long, val sessionCount: Int)

/** Reply of the shared `list` operation: the sessions stored on the terminal's cells and those cells. */
data class SharedSessionList(val sessions: List<SharedSessionInfo>, val cells: List<SharedCellInfo>)

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
