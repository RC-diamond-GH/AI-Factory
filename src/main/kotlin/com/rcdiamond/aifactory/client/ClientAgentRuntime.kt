package com.rcdiamond.aifactory.client

import com.rcdiamond.aifactory.client.agent.ReactAgent
import com.rcdiamond.aifactory.client.config.ClientConfigFiles
import com.rcdiamond.aifactory.client.config.ClientConfigurationException
import com.rcdiamond.aifactory.client.game.ClientGameActions
import com.rcdiamond.aifactory.client.llm.AgentChatSession
import com.rcdiamond.aifactory.client.llm.AgentUpdate
import com.rcdiamond.aifactory.client.llm.ChatEntry
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.model_provider.openai_completion.OpenAICompletion
import com.rcdiamond.aifactory.client.tool.ExecuteCommandTool
import com.rcdiamond.aifactory.client.tool.GiveItemTool
import com.rcdiamond.aifactory.client.tool.ToolManager
import com.rcdiamond.aifactory.client.ui.ToolUseSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraftforge.fml.loading.FMLPaths
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/** Only this client composition root knows which provider implements the abstract LLM API. */
object ClientAgentRuntime : AgentChatSession {
    private val gameDispatcher = object : CoroutineDispatcher() {
        override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Minecraft.getInstance().isSameThread
        override fun dispatch(context: CoroutineContext, block: Runnable) = Minecraft.getInstance().execute(block)
    }
    private var scope = CoroutineScope(SupervisorJob() + gameDispatcher)
    private var turn: Job? = null
    private var epoch = 0L
    private var agent: ReactAgent? = null
    private val mutableEntries = ArrayList<ChatEntry>()
    override val entries: List<ChatEntry> get() = mutableEntries
    override var revision: Long = 0
        private set
    override var busy: Boolean = false
        private set
    override var status: String = ""
        private set

    override fun submit(message: String) {
        if (busy || message.isBlank() || Minecraft.getInstance().player == null) return
        val session = epoch
        busy = true
        status = text("ai_factory.chat.thinking")
        append("user", message)
        turn = scope.launch {
            var assistantIndex = -1
            var toolIndex = -1
            try {
                val current = agent ?: configure()
                val answer = current.respond(message) { update ->
                    if (epoch != session) return@respond
                    when (update) {
                        is AgentUpdate.TextDelta -> {
                            if (assistantIndex < 0) assistantIndex = append("assistant", "")
                            val entry = mutableEntries[assistantIndex]
                            mutableEntries[assistantIndex] = entry.copy(text = entry.text + update.text)
                            revision++
                        }
                        is AgentUpdate.ToolStarted -> {
                            assistantIndex = -1
                            status = ToolUseSummary.running(update.name) { key, args -> text(key, *args) }
                            toolIndex = append("tool", status)
                        }
                        is AgentUpdate.ToolFinished -> {
                            val result = ToolUseSummary.finished(update.name, update.result) { key, args ->
                                text(key, *args)
                            }
                            if (toolIndex >= 0) {
                                mutableEntries[toolIndex] = ChatEntry("tool", result)
                                revision++
                            } else append("tool", result)
                            toolIndex = -1
                            status = text("ai_factory.chat.thinking")
                        }
                    }
                }
                if (epoch != session) return@launch
                if (assistantIndex < 0 && answer.isNotBlank()) append("assistant", answer)
                status = text("ai_factory.chat.ready")
            } catch (_: CancellationException) {
                if (epoch != session) return@launch
                status = text("ai_factory.chat.cancelled")
                append("system", status)
            } catch (failure: Exception) {
                if (epoch != session) return@launch
                status = when (failure) {
                    is ClientConfigurationException -> failure.message ?: text("ai_factory.chat.config_error")
                    is LLMAPIError -> failure.message ?: text("ai_factory.chat.request_error")
                    else -> text("ai_factory.chat.request_error")
                }
                append("system", status)
            } finally {
                if (epoch == session) {
                    busy = false
                    turn = null
                }
            }
        }
    }

    private suspend fun configure(): ReactAgent {
        val config = withContext(Dispatchers.IO) {
            ClientConfigFiles.load(FMLPaths.CONFIGDIR.get().resolve("ai-factory"))
        }
        val tools = ToolManager()
        tools.register(GiveItemTool(ClientGameActions))
        tools.register(ExecuteCommandTool(ClientGameActions))
        val provider = OpenAICompletion(
            config.model.baseUrl, config.model.apiKey, config.model.model,
            requestTimeout = Duration.ofSeconds(config.agent.requestTimeoutSeconds),
        )
        return ReactAgent(provider, tools, config.agent).also { agent = it }
    }

    override fun reload() {
        if (busy) return
        val session = epoch
        busy = true
        status = text("ai_factory.chat.loading")
        turn = scope.launch {
            try {
                configure()
                if (epoch != session) return@launch
                mutableEntries.clear()
                revision++
                status = text("ai_factory.chat.reloaded")
            } catch (_: CancellationException) {
                if (epoch != session) return@launch
                status = text("ai_factory.chat.cancelled")
            } catch (failure: Exception) {
                if (epoch != session) return@launch
                status = if (failure is ClientConfigurationException) failure.message ?: text("ai_factory.chat.config_error")
                else text("ai_factory.chat.config_error")
                append("system", status)
            } finally {
                if (epoch == session) {
                    busy = false
                    turn = null
                }
            }
        }
    }

    override fun cancelTurn() {
        turn?.cancel()
    }

    override fun clear() {
        if (busy) return
        agent?.reset()
        mutableEntries.clear()
        revision++
        status = text("ai_factory.chat.ready")
    }

    fun disconnect() {
        epoch++
        scope.cancel()
        ClientGameActions.disconnect()
        scope = CoroutineScope(SupervisorJob() + gameDispatcher)
        agent = null
        turn = null
        busy = false
        mutableEntries.clear()
        revision++
        status = ""
    }

    private fun append(speaker: String, content: String): Int {
        mutableEntries += ChatEntry(speaker, content)
        revision++
        return mutableEntries.lastIndex
    }

    private fun text(key: String, vararg args: Any): String = Component.translatable(key, *args).string
}
