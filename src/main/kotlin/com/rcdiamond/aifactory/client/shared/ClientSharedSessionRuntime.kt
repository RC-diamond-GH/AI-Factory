package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.addon.ae2.client.tool.Ae2Tools
import com.rcdiamond.aifactory.client.agent.ReactAgent
import com.rcdiamond.aifactory.client.config.ClientConfigFiles
import com.rcdiamond.aifactory.client.config.ClientConfigurationException
import com.rcdiamond.aifactory.client.llm.AgentChatSession
import com.rcdiamond.aifactory.client.llm.AgentSettings
import com.rcdiamond.aifactory.client.llm.AgentUpdate
import com.rcdiamond.aifactory.client.llm.ChatEntry
import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.client.llm.SharedCellInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionList
import com.rcdiamond.aifactory.client.model_provider.openai_completion.OpenAICompletion
import com.rcdiamond.aifactory.client.tool.ToolManager
import com.rcdiamond.aifactory.shared.SharedContextCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLPaths
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.CoroutineContext

private const val OP_LIST = "list"
private const val OP_CREATE = "create"
private const val OP_OPEN = "open"
private const val OP_SAVE = "save"
private const val OP_STATE = "state"
private const val OP_TOOL = "tool"

/** Ordinary request timeout; conversations use [SharedSessionRpc.BULK_TIMEOUT_MILLIS]. */
private const val REQUEST_TIMEOUT_MILLIS = 30_000L

/**
 * Client owner of the one shared ME session this player holds.
 *
 * The conversation lives on the server, and this runtime is the only place that keeps the client's copy of
 * it in sync: it opens or creates the session the server named, reuses the per-world cache whenever the
 * server's hash still matches, runs the ordinary [ReactAgent] loop locally with only ME terminal tools,
 * and saves the resulting context back before it reports that nothing is running any more.
 *
 * Lifetime rules, all expressed through the server's `state` operation:
 *  - closing the chat screen neither cancels the turn nor releases the session while it runs;
 *  - the session is only released once a save has been acknowledged and both the screen and the turn are
 *    inactive, so a close can never drop an unsaved conversation;
 *  - a save the server refuses (a full cell, a stale hash, a malformed context) keeps the session and the
 *    local conversation, and the next open or turn retries it instead of discarding either.
 *
 * The client never invents a session id, cell id or location the server did not return, and every operation
 * is addressed through the lock the server holds for this player; authority stays server side.
 *
 * ## Fail-closed limits
 * Between saves the conversation lives in memory only. A disconnect, a server restart (which drops the
 * server's leases) or a save that keeps being refused therefore abandons anything not yet acknowledged: the
 * next open takes the server's stored copy, and a turn that was running when the connection dropped is not
 * resumed. A refused save never releases the session, which means a session whose cell is full stays held
 * until a save succeeds or the player clears its conversation; a lease the server has already dropped is
 * reported as an error instead of being silently replaced. The per-world cache is capped, and dropping an
 * entry only costs one server fetch. The chat log is presentation only: reopening a session rebuilds the
 * model history from the server or cache, so closing the screen never loses recorded messages.
 */
object ClientSharedSessionRuntime : AgentChatSession {
    /**
     * Composition dispatcher for this client runtime, the same contract as the local chat runtime and kept
     * local so neither runtime depends on the other's lifecycle.
     */
    private val clientDispatcher = object : CoroutineDispatcher() {
        override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Minecraft.getInstance().isSameThread
        override fun dispatch(context: CoroutineContext, block: Runnable) = Minecraft.getInstance().execute(block)
    }

    private var scope = CoroutineScope(SupervisorJob() + clientDispatcher)

    /** Serializes every exchange that changes the server's view of the open session. */
    private var commands = Mutex()

    private val cache = SharedSessionCache()
    private var epoch = 0L
    private var turn: Job? = null
    private var agent: ReactAgent? = null
    private var titleGenerator: SharedSessionTitleGenerator? = null
    private var settings: AgentSettings? = null
    private var active: Active? = null
    private var lastSaveFailure: String? = null
    private val mutableEntries = ArrayList<ChatEntry>()

    override val entries: List<ChatEntry> get() = mutableEntries
    override var revision: Long = 0
        private set
    override var busy: Boolean = false
        private set
    override var status: String = ""
        private set
    override val conversationTitle: String? get() = active?.title

    /** A shared turn survives its screen; only the server decides when the session may be released. */
    override val cancelTurnOnClose: Boolean get() = false

    /** One session this client has open and the server still associates with this player. */
    private class Active(
        val id: UUID,
        var title: String,
        var autoTitle: Boolean,
        var serverHash: String,
        var tokens: Long,
        var screenOpen: Boolean,
        var running: Boolean,
        var pendingTitle: String? = null,
    )

    // ----------------------------------------------------------------- events

    /** The server validated a terminal use and lets this client pick a session for that terminal. */
    fun onTerminalOpen(terminal: JsonObject) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null) return
        if (minecraft.screen is SharedSessionSelectionScreen) return
        minecraft.setScreen(SharedSessionSelectionScreen(terminal))
    }

    fun onResponse(requestId: UUID, payload: JsonObject) {
        SharedSessionRpc.onResponse(requestId, payload)
    }

    /** The connection is gone: nothing may be sent any more, but the per-world cache stays on disk. */
    fun disconnect() {
        epoch++
        turn?.cancel()
        turn = null
        SharedSessionRpc.disconnect()
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + clientDispatcher)
        commands = Mutex()
        agent = null
        titleGenerator = null
        settings = null
        active = null
        lastSaveFailure = null
        busy = false
        mutableEntries.clear()
        revision++
        status = ""
    }

    // -------------------------------------------------------- terminal screen

    /** Lists the sessions and cells of [terminal]; the callback runs on the client thread. */
    fun list(terminal: JsonObject, onResult: (SharedSessionList?, String?) -> Unit) {
        val generation = epoch
        scope.launch {
            try {
                val response = rpc(OP_LIST, buildJsonObject { put("terminal", terminal) })
                val parsed = parseList(response)
                if (epoch == generation) onResult(parsed, null)
            } catch (_: CancellationException) {
                // Disconnected; the screen went with the connection.
            } catch (failure: SharedSessionException) {
                if (epoch == generation) onResult(null, failure.message ?: text("ai_factory.shared.list_failed"))
            } catch (failure: Exception) {
                if (epoch == generation) onResult(null, text("ai_factory.shared.list_failed"))
            }
        }
    }

    /** Creates on [cellId] or another mounted cell with room, then opens it on the client thread. */
    fun create(terminal: JsonObject, cellId: UUID, title: String, onResult: (SharedSessionInfo?, String?) -> Unit) {
        val generation = epoch
        val requested = title.trim().ifEmpty { text("ai_factory.shared.untitled") }
        scope.launch {
            var created: SharedSessionInfo? = null
            val error = try {
                // Only build the agent when no session is being replaced: a takeover flushes the session it
                // holds, so the configuration is checked again inside the open that follows the create.
                if (active == null) configureAgent()
                val response = rpc(
                    OP_CREATE,
                    buildJsonObject {
                        put("terminal", terminal)
                        put("cellId", cellId.toString())
                        put("title", requested)
                        put("autoTitle", title.isBlank())
                    },
                    SharedSessionRpc.BULK_TIMEOUT_MILLIS,
                )
                val id = response.stringField("id")?.let(::parseUuid)
                    ?: throw SharedSessionException(text("ai_factory.shared.malformed_reply"))
                val actualCellId = response.stringField("cellId")?.let(::parseUuid)
                    ?: throw SharedSessionException(text("ai_factory.shared.malformed_reply"))
                val info = SharedSessionInfo(id, actualCellId, requested, "", 0L, autoTitle = title.isBlank())
                created = info
                commands.withLock { resolveOpen(terminal, info) }
            } catch (_: CancellationException) {
                return@launch
            } catch (failure: Exception) {
                messageFor(failure)
            }
            if (epoch != generation) return@launch
            if (error == null) onResult(created, null) else onResult(null, error)
        }
    }

    /** Opens a session, reusing it when this client already holds it. The callback runs on the client thread. */
    fun open(terminal: JsonObject, session: SharedSessionInfo, onResult: (String?) -> Unit) {
        val generation = epoch
        scope.launch {
            val error = try {
                commands.withLock { resolveOpen(terminal, session) }
            } catch (_: CancellationException) {
                return@launch
            } catch (failure: Exception) {
                messageFor(failure)
            }
            if (epoch == generation) onResult(error)
        }
    }

    // -------------------------------------------------------------- HUD state

    /** True while a session is open but its chat screen is not the visible screen. */
    fun hudVisible(): Boolean = active?.screenOpen == false

    fun hudTitle(): String = active?.title ?: ""

    /** Server-reported token count of the last synchronized conversation, for charge feedback. */
    fun hudTokens(): Long = active?.tokens ?: 0L

    fun hudRunning(): Boolean = active?.running == true

    fun isActive(id: UUID): Boolean = active?.id == id

    /** Last assistant line of the log, or null before the first answer. */
    fun hudTail(): String? = mutableEntries.lastOrNull { it.speaker == "assistant" && it.text.isNotBlank() }?.text

    // -------------------------------------------------------- AgentChatSession

    override fun submit(message: String) {
        val session = active ?: return
        if (busy || turn != null || message.isBlank() || Minecraft.getInstance().player == null) return
        val generation = epoch
        busy = true
        status = text("ai_factory.shared.thinking")
        append("user", message)
        turn = scope.launch {
            try {
                session.running = true
                if (!announceState(session)) throw SharedSessionException(text("ai_factory.shared.open_failed"))
                var assistantIndex = -1
                var toolIndex = -1
                val current = agent ?: configureAgent()
                val answer = current.respond(message) { update ->
                    if (epoch != generation) return@respond
                    when (update) {
                        is AgentUpdate.TextDelta -> {
                            if (assistantIndex < 0) assistantIndex = append("assistant", "")
                            val entry = mutableEntries[assistantIndex]
                            mutableEntries[assistantIndex] = entry.copy(text = entry.text + update.text)
                            revision++
                        }
                        is AgentUpdate.ToolStarted -> {
                            assistantIndex = -1
                            status = text("ai_factory.chat.tool_running", update.name)
                            toolIndex = append("tool", status)
                        }
                        is AgentUpdate.ToolFinished -> {
                            val result = "${update.name}: ${update.result}"
                            if (toolIndex >= 0) {
                                mutableEntries[toolIndex] = ChatEntry("tool", result)
                                revision++
                            } else append("tool", result)
                            toolIndex = -1
                            status = text("ai_factory.shared.thinking")
                        }
                    }
                }
                if (epoch != generation) return@launch
                if (assistantIndex < 0 && answer.isNotBlank()) append("assistant", answer)
                if (session.autoTitle && session.pendingTitle == null) {
                    try {
                        session.pendingTitle = checkNotNull(titleGenerator).generate(current.history)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        append("system", text("ai_factory.shared.title_failed"))
                    }
                }
                status = text("ai_factory.shared.ready")
            } catch (_: CancellationException) {
                if (epoch != generation) return@launch
                status = text("ai_factory.chat.cancelled")
                append("system", status)
            } catch (failure: Exception) {
                if (epoch != generation) return@launch
                status = messageFor(failure)
                append("system", status)
            } finally {
                session.running = false
                if (epoch == generation) {
                    // The history may have changed even when the turn failed or was cancelled, so the durable
                    // copy is written here, outside the cancelled scope, before the session can be released.
                    try {
                        withContext(NonCancellable) { finishTurn(session) }
                    } catch (failure: Exception) {
                        status = text("ai_factory.shared.save_failed")
                    } finally {
                        if (epoch == generation) {
                            busy = false
                            turn = null
                        }
                    }
                }
            }
        }
    }

    override fun reload() {
        if (busy || turn != null) return
        val generation = epoch
        busy = true
        status = text("ai_factory.chat.loading")
        scope.launch {
            try {
                val carried = agent?.exportContext()
                val fresh = configureAgent()
                val local = settings
                if (carried != null && local != null) {
                    fresh.importContext(carried.copy(
                        systemPrompt = local.systemPrompt,
                        tools = fresh.exportContext().tools,
                    ))
                }
                if (epoch == generation) status = text("ai_factory.shared.reloaded")
            } catch (_: CancellationException) {
                // Disconnected.
            } catch (failure: Exception) {
                if (epoch == generation) status = messageFor(failure)
            } finally {
                if (epoch == generation) busy = false
            }
        }
    }

    override fun cancelTurn() {
        turn?.cancel()
    }

    override fun clear() {
        if (busy || turn != null) return
        val session = active ?: return
        agent?.reset()
        mutableEntries.clear()
        revision++
        status = text("ai_factory.shared.cleared")
        val generation = epoch
        scope.launch {
            commands.withLock {
                if (epoch != generation || active !== session) return@launch
                // An empty conversation is still a conversation: it is saved so the server copy matches.
                if (!flush(session)) status = saveStatus("ai_factory.shared.save_failed")
            }
        }
    }

    override fun onScreenOpened() {
        val session = active ?: return
        session.screenOpen = true
        val generation = epoch
        scope.launch {
            commands.withLock {
                if (epoch != generation || active !== session) return@launch
                if (!session.screenOpen) return@launch
                announceState(session)
                if (!session.running) flush(session)
            }
        }
    }

    override fun onScreenClosed() {
        val session = active ?: return
        session.screenOpen = false
        val generation = epoch
        scope.launch {
            commands.withLock {
                if (epoch != generation || active !== session) return@launch
                // A screen that was reopened before this ran keeps its session.
                if (session.screenOpen) return@launch
                if (session.running) {
                    // The turn keeps running in the background; the server must not release the session.
                    announceState(session)
                    return@launch
                }
                if (!flush(session)) {
                    status = saveStatus("ai_factory.shared.unsaved")
                    return@launch
                }
                if (!session.screenOpen && !session.running && releaseState(session)) active = null
            }
        }
    }

    // -------------------------------------------------------------- open paths

    private suspend fun resolveOpen(terminal: JsonObject, session: SharedSessionInfo): String? {
        val existing = active
        if (existing != null && existing.id == session.id) {
            // Same session, still held: reopening only has to say so and retry an earlier save. A failed save
            // does not block the screen, so the player can still see the conversation and clear it.
            existing.screenOpen = true
            status = text("ai_factory.shared.ready")
            if (!announceState(existing)) return text("ai_factory.shared.open_failed")
            if (!existing.running && !flush(existing)) status = saveStatus("ai_factory.shared.unsaved")
            return null
        }
        if (existing != null) {
            if (existing.running) return text("ai_factory.shared.running_blocks_switch")
            if (!flush(existing)) return saveStatus("ai_factory.shared.unsaved_blocks_switch")
            if (!releaseState(existing)) return text("ai_factory.shared.release_failed")
            active = null
        }
        return acquireLocked(terminal, session)
    }

    /**
     * Acquires [session] on [terminal]. Loads the client configuration first, so a broken client config fails
     * before any server state changes; then reuses the per-world cache unless the server reports a different
     * hash; then adopts the conversation so the ReAct loop continues where it stopped.
     */
    private suspend fun acquireLocked(terminal: JsonObject, session: SharedSessionInfo): String? {
        val built = configureAgent()
        val world = worldKey()
        val cached = cache.read(world, session.id)?.takeIf {
            runCatching { SharedContextCodec.hash(it.context) == it.hash }.getOrDefault(false)
        }
        try {
            return fetchLocked(terminal, session, built, world, cached)
        } finally {
            if (active?.id != session.id) {
                withContext(NonCancellable) {
                    runCatching {
                        rpc(OP_STATE, buildJsonObject {
                            put("id", session.id.toString())
                            put("screenOpen", false)
                            put("running", false)
                        })
                    }
                }
            }
        }
    }

    private suspend fun fetchLocked(
        terminal: JsonObject,
        session: SharedSessionInfo,
        built: ReactAgent,
        world: String,
        cached: CachedSharedSession?,
    ): String? {
        var response = rpc(
            OP_OPEN,
            openPayload(terminal, session.id, cached?.hash ?: ""),
            SharedSessionRpc.BULK_TIMEOUT_MILLIS,
        )
        var serverHash = response.stringField("hash")
            ?: throw SharedSessionException(text("ai_factory.shared.malformed_reply"))
        var tokens = response.longField("tokens") ?: 0L
        val returned = (response["context"] as? JsonObject)?.takeIf { it.isNotEmpty() }

        var conversation: LLMContext? = null
        if (returned != null) {
            conversation = decodeContext(returned) ?: return text("ai_factory.shared.incompatible_context")
            cache.write(world, session.id, CachedSharedSession(serverHash, tokens, returned))
        } else if (cached != null) {
            conversation = decodeContext(cached.context)
            if (conversation == null) {
                // The server agreed with the hash this client sent, so it sent no context, yet the local copy
                // is unusable. Recovery is asking again with no hash and taking the full conversation.
                response = rpc(
                    OP_OPEN,
                    openPayload(terminal, session.id, ""),
                    SharedSessionRpc.BULK_TIMEOUT_MILLIS,
                )
                serverHash = response.stringField("hash")
                    ?: throw SharedSessionException(text("ai_factory.shared.malformed_reply"))
                tokens = response.longField("tokens") ?: 0L
                val forced = (response["context"] as? JsonObject)?.takeIf { it.isNotEmpty() }
                    ?: return text("ai_factory.shared.incompatible_context")
                conversation = decodeContext(forced) ?: return text("ai_factory.shared.incompatible_context")
                cache.write(world, session.id, CachedSharedSession(serverHash, tokens, forced))
            }
        }

        // A brand-new session stores no conversation. A blank one stays a fresh conversation with this
        // client's own prompt instead of importing an empty prompt.
        if (conversation != null && !isBlankConversation(conversation)) {
            // Preserve history/prompts, but old saved schemas must not resurrect removed tools or hide new ones.
            built.importContext(conversation.copy(tools = built.exportContext().tools))
        }
        mutableEntries.clear()
        for (message in built.history) {
            when (message) {
                is ChatMessage.User -> mutableEntries += ChatEntry("user", message.content)
                is ChatMessage.Assistant -> {
                    message.content?.takeIf { it.isNotEmpty() }?.let {
                        mutableEntries += ChatEntry("assistant", it)
                    }
                    for (call in message.toolCalls) {
                        mutableEntries += ChatEntry("tool", "${call.name}: ${call.arguments}")
                    }
                }
                is ChatMessage.Tool -> mutableEntries += ChatEntry("tool", message.content)
            }
        }
        revision++
        active = Active(
            session.id,
            response.stringField("title") ?: throw malformed(),
            (response["autoTitle"] as? JsonPrimitive)?.booleanOrNull ?: throw malformed(),
            serverHash, tokens, screenOpen = true, running = false,
        )
        status = text("ai_factory.shared.ready")
        return null
    }

    // ---------------------------------------------------------------- exchange

    /** Ends a finished turn: write the conversation first, then report the turn as stopped. */
    private suspend fun finishTurn(session: Active) {
        commands.withLock {
            if (active !== session) return
            if (!flush(session)) {
                status = saveStatus("ai_factory.shared.save_failed")
                return
            }
            if (!session.screenOpen && !session.running) {
                if (releaseState(session)) active = null
            } else {
                announceState(session)
            }
        }
    }

    /**
     * Ensures the server holds the current conversation. Returns true when the conversation is durable: either
     * the local hash already equals the server's own hash of the context it stores, or the server
     * acknowledged this save. A refusal leaves both the local conversation and the session untouched.
     */
    private suspend fun flush(session: Active): Boolean {
        lastSaveFailure = null
        val current = agent ?: return true
        val context = try {
            val snapshot = current.exportContext()
            withContext(Dispatchers.Default) { SharedContextCodec.encode(snapshot) }
        } catch (failure: Exception) {
            return false
        }
        val hash = try {
            withContext(Dispatchers.Default) { SharedContextCodec.hash(context) }
        } catch (failure: Exception) {
            return false
        }
        if (hash == session.serverHash && session.pendingTitle == null) return true
        return try {
            val response = rpc(
                OP_SAVE,
                buildJsonObject {
                    put("id", session.id.toString())
                    put("hash", session.serverHash)
                    put("context", context)
                    session.pendingTitle?.let { put("title", it) }
                },
                SharedSessionRpc.BULK_TIMEOUT_MILLIS,
            )
            val acknowledged = response.stringField("hash") ?: return false
            val title = response.stringField("title") ?: return false
            val autoTitle = (response["autoTitle"] as? JsonPrimitive)?.booleanOrNull ?: return false
            session.title = title
            session.autoTitle = autoTitle
            session.pendingTitle = null
            session.serverHash = acknowledged
            session.tokens = response.longField("tokens") ?: 0L
            cache.write(worldKey(), session.id, CachedSharedSession(acknowledged, session.tokens, context))
            true
        } catch (failure: SharedSessionException) {
            lastSaveFailure = failure.message
            false
        }
    }

    private fun saveStatus(key: String): String =
        lastSaveFailure?.let { "${text(key)} $it" } ?: text(key)

    private suspend fun announceState(session: Active): Boolean = try {
        rpc(
            OP_STATE,
            buildJsonObject {
                put("id", session.id.toString())
                put("screenOpen", session.screenOpen)
                put("running", session.running)
            },
        )
        true
    } catch (failure: SharedSessionException) {
        false
    }

    private suspend fun releaseState(session: Active): Boolean = try {
        rpc(
            OP_STATE,
            buildJsonObject {
                put("id", session.id.toString())
                put("screenOpen", false)
                put("running", false)
            },
        )
        true
    } catch (failure: SharedSessionException) {
        status = failure.message ?: text("ai_factory.shared.release_failed")
        false
    }

    /**
     * Gateway the shared client tools use: one terminal operation against the currently held session.
     *
     * Failures never throw: a missing session and a failed exchange come back as the `ok:false` object the
     * tools expect, because an exception would fail the agent's whole turn instead of telling the model what
     * happened.
     */
    private suspend fun sendTool(operation: String, arguments: JsonObject): JsonObject {
        val session = active
            ?: return failurePayload("no_terminal_session", text("ai_factory.shared.no_session"))
        return try {
            SharedSessionRpc.call(
                OP_TOOL,
                buildJsonObject {
                    put("id", session.id.toString())
                    put("operation", operation)
                    put("arguments", arguments)
                },
            )
        } catch (failure: SharedSessionException) {
            failurePayload("request_failed", failure.message ?: text("ai_factory.shared.tool_failed"))
        }
    }

    private suspend fun rpc(
        operation: String,
        payload: JsonObject,
        timeoutMillis: Long = REQUEST_TIMEOUT_MILLIS,
    ): JsonObject {
        val response = SharedSessionRpc.call(operation, payload, timeoutMillis)
        val ok = (response["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
        if (!ok) throw SharedSessionException(response.stringField("error") ?: text("ai_factory.shared.rejected"))
        return response
    }

    // ------------------------------------------------------------- composition

    /**
     * Loads the client configuration and rebuilds provider, tools and agent with only ME terminal tools.
     * The local chat's item grants and arbitrary commands are intentionally unavailable to this assistant.
     */
    private suspend fun configureAgent(): ReactAgent {
        val config = withContext(Dispatchers.IO) {
            ClientConfigFiles.load(FMLPaths.CONFIGDIR.get().resolve("ai-factory"))
        }
        val manager = ToolManager()
        if (ModList.get().isLoaded("ae2")) {
            // Addon class only: Ae2Tools contains no AE2 API, so no AE2 type is linked from here.
            Ae2Tools.register(manager, ::sendTool)
        }
        val provider = OpenAICompletion(
            config.model.baseUrl, config.model.apiKey, config.model.model,
            requestTimeout = Duration.ofSeconds(config.agent.requestTimeoutSeconds),
        )
        val built = ReactAgent(
            provider,
            manager,
            config.agent,
        )
        settings = config.agent
        titleGenerator = SharedSessionTitleGenerator(provider)
        agent = built
        return built
    }

    // ---------------------------------------------------------------- parsing

    private fun parseList(response: JsonObject): SharedSessionList {
        val sessions = (response["sessions"] as? JsonArray)?.map { entry ->
            val json = entry as? JsonObject ?: throw SharedSessionException(text("ai_factory.shared.malformed_reply"))
            SharedSessionInfo(
                id = json.stringField("id")?.let(::parseUuid) ?: throw malformed(),
                cellId = json.stringField("cellId")?.let(::parseUuid) ?: throw malformed(),
                title = json.stringField("title") ?: "",
                hash = json.stringField("hash") ?: "",
                tokens = json.longField("tokens") ?: 0L,
                autoTitle = (json["autoTitle"] as? JsonPrimitive)?.booleanOrNull ?: throw malformed(),
            )
        } ?: emptyList()
        val cells = (response["cells"] as? JsonArray)?.map { entry ->
            val json = entry as? JsonObject ?: throw malformed()
            SharedCellInfo(
                id = json.stringField("id")?.let(::parseUuid) ?: throw malformed(),
                bytes = json.longField("bytes") ?: 0L,
                usedBytes = json.longField("usedBytes") ?: 0L,
                sessionCount = (json.longField("sessionCount") ?: 0L).toInt(),
            )
        } ?: emptyList()
        return SharedSessionList(sessions, cells)
    }

    private fun openPayload(terminal: JsonObject, id: UUID, hash: String): JsonObject = buildJsonObject {
        put("terminal", terminal)
        put("id", id.toString())
        put("hash", hash)
    }

    private fun decodeContext(json: JsonObject): LLMContext? = try {
        SharedContextCodec.decode(json)
    } catch (failure: Exception) {
        null
    }

    private fun isBlankConversation(context: LLMContext): Boolean =
        context.messages.isEmpty() && context.systemPrompt.isBlank()

    private fun malformed(): SharedSessionException = SharedSessionException(text("ai_factory.shared.malformed_reply"))

    private fun parseUuid(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (failure: IllegalArgumentException) {
        null
    }

    private fun failurePayload(error: String, message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", error)
        put("message", message)
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The world this client is in, as a cache key: a multiplayer server by its address, a singleplayer world
     * by its save folder, so two worlds never share a cached shared session.
     */
    private fun worldKey(): String {
        val minecraft = Minecraft.getInstance()
        minecraft.currentServer?.let { return "server_" + SharedSessionCache.sanitize(it.ip) }
        val integrated = minecraft.singleplayerServer
        if (integrated != null) {
            val name = runCatching { integrated.getWorldPath(LevelResource.ROOT) }.getOrNull()
                ?.fileName?.toString()
            if (!name.isNullOrBlank()) return "world_" + SharedSessionCache.sanitize(name)
        }
        val dimension = minecraft.level?.dimension()?.location()?.toString() ?: "unknown"
        return "unbound_" + SharedSessionCache.sanitize(dimension)
    }

    private fun append(speaker: String, content: String): Int {
        mutableEntries += ChatEntry(speaker, content)
        revision++
        return mutableEntries.lastIndex
    }

    private fun messageFor(failure: Exception): String = when (failure) {
        is ClientConfigurationException -> failure.message ?: text("ai_factory.chat.config_error")
        is SharedSessionException -> failure.message ?: text("ai_factory.shared.error")
        is LLMAPIError -> failure.message ?: text("ai_factory.chat.request_error")
        else -> text("ai_factory.chat.request_error")
    }

    private fun text(key: String, vararg args: Any): String = Component.translatable(key, *args).string
}

private fun JsonObject.stringField(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.longField(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull
