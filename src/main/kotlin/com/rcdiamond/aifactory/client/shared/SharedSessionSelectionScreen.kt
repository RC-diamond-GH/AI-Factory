package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.client.llm.SharedCellInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionList
import com.rcdiamond.aifactory.client.ui.AgentChatScreen
import kotlinx.serialization.json.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Session picker for one validated ME terminal, opened by the server's terminal packet.
 *
 * It only ever shows what the `list` reply for that terminal contains: the sessions stored on its cells and
 * the intelligent cells themselves. Creating a session names one of those cells and a title; opening one hands
 * the session to [ClientSharedSessionRuntime], which owns the lock, the conversation and the chat screen from
 * then on. Nothing is guessed locally, so a session the server did not return cannot be selected.
 */
class SharedSessionSelectionScreen(private val terminal: JsonObject) :
    Screen(Component.translatable("ai_factory.shared.title")) {
    private var requested = false
    private var loading = true
    private var pendingAction = false
    private var error: String? = null
    private var list: SharedSessionList? = null
    private var page = 0
    private var cellIndex = 0
    private lateinit var titleInput: EditBox


    override fun isPauseScreen(): Boolean = false

    override fun added() {
        super.added()
        ClientSharedSessionRuntime.onScreenOpened()
    }

    override fun removed() {
        ClientSharedSessionRuntime.onScreenClosed()
        super.removed()
    }
    override fun init() {
        val draft = if (::titleInput.isInitialized) titleInput.value else ""
        val sessions = list?.sessions.orEmpty()
        val cells = list?.cells.orEmpty()
        if (cells.isNotEmpty()) cellIndex %= cells.size
        val pages = pageCount(sessions.size)
        page = page.coerceIn(0, pages - 1)
        val right = (width - 136).coerceAtLeast(180)
        val sessionWidth = (right - 40).coerceAtLeast(80)

        val first = page * PAGE_SIZE
        for (offset in 0 until PAGE_SIZE) {
            val session = sessions.getOrNull(first + offset) ?: break
            val marker = if (ClientSharedSessionRuntime.isActive(session.id)) "» " else ""
            val label = "$marker${session.title}  ·  ${session.tokens}"
            val button = addRenderableWidget(
                Button.builder(Component.literal(truncate(label, sessionWidth - 8))) { openSession(session) }
                    .bounds(16, SESSIONS_Y + offset * ROW_HEIGHT, sessionWidth, BUTTON_HEIGHT)
                    .build(),
            )
            button.active = !pendingAction
        }

        titleInput = EditBox(
            font, right, SESSIONS_Y, 120, BUTTON_HEIGHT,
            Component.translatable("ai_factory.shared.session_title"),
        )
        titleInput.setMaxLength(MAX_TITLE_LENGTH)
        titleInput.value = draft
        addRenderableWidget(titleInput)

        val cell = cells.getOrNull(cellIndex)
        val cellButton = addRenderableWidget(
            Button.builder(Component.translatable("ai_factory.shared.cell", cellLabel(cell))) {
                if (cells.isNotEmpty()) {
                    cellIndex = (cellIndex + 1) % cells.size
                    rebuildWidgets()
                }
            }.bounds(right, SESSIONS_Y + ROW_HEIGHT, 120, BUTTON_HEIGHT).build(),
        )
        cellButton.active = cells.size > 1
        val create = addRenderableWidget(
            Button.builder(Component.translatable("ai_factory.shared.create")) { createSession(cell) }
                .bounds(right, SESSIONS_Y + ROW_HEIGHT * 2, 120, BUTTON_HEIGHT).build(),
        )
        create.active = cell != null && !pendingAction && !ClientSharedSessionRuntime.hudRunning()

        val bottom = height - 26
        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) { onClose() }
                .bounds(16, bottom, 80, BUTTON_HEIGHT).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable("ai_factory.shared.refresh")) { request() }
                .bounds(104, bottom, 80, BUTTON_HEIGHT).build(),
        )
        val previous = addRenderableWidget(
            Button.builder(Component.literal("<")) {
                page--
                rebuildWidgets()
            }.bounds(right, bottom, 24, BUTTON_HEIGHT).build(),
        )
        previous.active = page > 0
        val next = addRenderableWidget(
            Button.builder(Component.literal(">")) {
                page++
                rebuildWidgets()
            }.bounds(right + 28, bottom, 24, BUTTON_HEIGHT).build(),
        )
        next.active = page < pages - 1

        if (!requested) {
            requested = true
            request()
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        graphics.drawString(font, title, 16, 12, 0xFFFFFF)
        val message = error
        if (message != null) {
            graphics.drawString(font, truncate(message, width - 32), 16, 26, 0xFF5555)
        } else if (loading) {
            graphics.drawString(font, Component.translatable("ai_factory.shared.loading"), 16, 26, 0xB0B0B0)
        } else {
            graphics.drawString(
                font,
                Component.translatable(
                    "ai_factory.shared.counts",
                    list?.sessions?.size ?: 0,
                    list?.cells?.size ?: 0,
                ),
                16, 26, 0xB0B0B0,
            )
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun request() {
        loading = true
        error = null
        ClientSharedSessionRuntime.list(terminal) { result, failure ->
            if (Minecraft.getInstance().screen !== this) return@list
            loading = false
            error = failure
            if (result != null) {
                list = result
                page = 0
            }
            rebuildWidgets()
        }
    }

    private fun createSession(cell: SharedCellInfo?) {
        if (cell == null || pendingAction) return
        pendingAction = true
        rebuildWidgets()
        error = null
        ClientSharedSessionRuntime.create(terminal, cell.id, titleInput.value) { info, failure ->
            pendingAction = false
            if (Minecraft.getInstance().screen !== this) {
                if (failure == null && Minecraft.getInstance().screen !is SharedSessionSelectionScreen) {
                    ClientSharedSessionRuntime.onScreenClosed()
                }
                return@create
            }
            if (failure != null) {
                error = failure
                rebuildWidgets()
                return@create
            }
            if (info != null) openChat()
        }
    }

    private fun openSession(session: SharedSessionInfo) {
        if (pendingAction) return
        pendingAction = true
        rebuildWidgets()
        error = null
        ClientSharedSessionRuntime.open(terminal, session) { failure ->
            pendingAction = false
            if (Minecraft.getInstance().screen !== this) {
                if (failure == null && Minecraft.getInstance().screen !is SharedSessionSelectionScreen) {
                    ClientSharedSessionRuntime.onScreenClosed()
                }
                return@open
            }
            if (failure != null) {
                error = failure
                rebuildWidgets()
                return@open
            }
            openChat()
        }
    }

    private fun openChat() {
        Minecraft.getInstance().setScreen(
            AgentChatScreen(ClientSharedSessionRuntime, Component.translatable("ai_factory.shared.chat_title")),
        )
    }

    private fun cellLabel(cell: SharedCellInfo?): String {
        if (cell == null) return Component.translatable("ai_factory.shared.no_cell").string
        return "${cell.id.toString().take(8)}  ${cell.usedBytes}/${cell.bytes}B  #${cell.sessionCount}"
    }

    private fun pageCount(size: Int): Int = if (size <= 0) 1 else (size + PAGE_SIZE - 1) / PAGE_SIZE

    private fun truncate(text: String, limit: Int): String = font.plainSubstrByWidth(text, limit.coerceAtLeast(16))

    private companion object {
        const val PAGE_SIZE = 5
        const val ROW_HEIGHT = 22
        const val BUTTON_HEIGHT = 18
        const val SESSIONS_Y = 48
        const val MAX_TITLE_LENGTH = 64
    }
}
