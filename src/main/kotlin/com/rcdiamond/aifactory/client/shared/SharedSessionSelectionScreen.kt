package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.client.llm.SharedCellInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionInfo
import com.rcdiamond.aifactory.client.llm.SharedSessionList
import com.rcdiamond.aifactory.client.ui.AgentChatScreen
import com.rcdiamond.aifactory.client.ui.FlatButton
import com.rcdiamond.aifactory.client.ui.FlatEditBox
import com.rcdiamond.aifactory.client.ui.TerminalUi
import kotlinx.serialization.json.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.UUID

/** Locally reachable terminal browser. Every mutation remains server-authoritative. */
class SharedSessionSelectionScreen(private val terminal: JsonObject) :
    Screen(Component.translatable("ai_factory.shared.title")) {
    private var requested = false
    private var loading = true
    private var pendingAction = false
    private var error: String? = null
    private var list: SharedSessionList? = null
    private var cellIndex = 0
    private var selectedId: UUID? = null
    private var confirmDelete: SharedSessionInfo? = null
    private var openingChat = false
    private val openLabel = Component.translatable("ai_factory.shared.open")
    private val confirmDeleteLabel = Component.translatable("ai_factory.shared.confirm_delete")
    private val deleteLabel = Component.translatable("ai_factory.shared.delete")
    private val cancelLabel = Component.translatable("gui.cancel")
    private lateinit var search: EditBox
    private lateinit var titleInput: EditBox
    private lateinit var sessions: SharedSessionListWidget
    private lateinit var open: FlatButton
    private lateinit var delete: FlatButton
    private lateinit var create: FlatButton
    private lateinit var refresh: FlatButton
    private lateinit var cellButton: FlatButton
    private val sidebarX get() = width - 148

    override fun isPauseScreen(): Boolean = false

    override fun added() {
        super.added()
        openingChat = false
        ClientSharedSessionRuntime.onScreenOpened()
    }

    override fun removed() {
        if (!openingChat) ClientSharedSessionRuntime.onScreenClosed()
        super.removed()
    }

    override fun init() {
        val draft = if (::titleInput.isInitialized) titleInput.value else ""
        val query = if (::search.isInitialized) search.value else ""
        val cells = list?.cells.orEmpty()
        if (cells.isNotEmpty()) cellIndex %= cells.size
        val right = sidebarX
        search = FlatEditBox(font, 16, 54, right - 28, 20, Component.translatable("ai_factory.shared.search"))
        search.setMaxLength(128)
        search.setHint(Component.translatable("ai_factory.shared.search"))
        search.value = query
        addRenderableWidget(search)
        sessions = addRenderableWidget(SharedSessionListWidget(16, 82, right - 28, height - 116, font,
            { selected ->
                selectedId = selected?.id
                confirmDelete = null
                refreshButtons()
            }, { selected -> openSession(selected) }))
        titleInput = FlatEditBox(font, right, 68, 132, 20, Component.translatable("ai_factory.shared.session_title"))
        titleInput.setMaxLength(64)
        titleInput.setHint(Component.translatable("ai_factory.shared.optional_title"))
        titleInput.value = draft
        addRenderableWidget(titleInput)
        val cell = cells.getOrNull(cellIndex)
        cellButton = button(right, 94, 132, Component.literal(cellLabel(cell))) {
            if (cells.isNotEmpty()) { cellIndex = (cellIndex + 1) % cells.size; rebuildWidgets() }
        }
        create = button(right, 120, 132, Component.translatable("ai_factory.shared.create")) { createSession(cell) }
        open = button(right, height - 76, 132, Component.translatable("ai_factory.shared.open")) {
            val confirmation = confirmDelete
            if (confirmation != null) deleteSession(confirmation) else sessions.selected?.let(::openSession)
        }
        delete = button(right, height - 52, 132, Component.translatable("ai_factory.shared.delete"), true) {
            if (confirmDelete != null) confirmDelete = null else confirmDelete = sessions.selected
            refreshButtons()
        }
        button(16, height - 26, 72, Component.translatable("gui.done")) { onClose() }
        refresh = button(94, height - 26, 72, Component.translatable("ai_factory.shared.refresh")) { request() }
        search.setResponder { applyFilter() }
        applyFilter()
        setInitialFocus(search)
        if (!requested) { requested = true; request() }
        refreshButtons()
    }

    private fun button(x: Int, y: Int, width: Int, label: Component, destructive: Boolean = false, action: () -> Unit): FlatButton =
        addRenderableWidget(FlatButton(x, y, width, 20, label, { action() }, destructive))

    private fun applyFilter() {
        val query = search.value.trim()
        val values = list?.sessions.orEmpty()
        sessions.update(if (query.isEmpty()) values else values.filter {
            it.title.contains(query, ignoreCase = true) || it.id.toString().contains(query, ignoreCase = true)
        }, selectedId)
    }

    override fun tick() {
        search.tick()
        titleInput.tick()
        refreshButtons()
    }

    private fun refreshButtons() {
        if (!::open.isInitialized) return
        val idle = !loading && !pendingAction
        val selected = sessions.selected
        open.active = idle && selected != null
        delete.active = idle && selected != null && !(ClientSharedSessionRuntime.isActive(selected.id) && ClientSharedSessionRuntime.busy)
        create.active = idle && list?.cells?.isNotEmpty() == true && !ClientSharedSessionRuntime.hudRunning()
        refresh.active = idle
        cellButton.active = idle && (list?.cells?.size ?: 0) > 1
        sessions.active = idle
        search.setEditable(idle)
        titleInput.setEditable(idle)
        open.message = if (confirmDelete == null) openLabel else confirmDeleteLabel
        delete.message = if (confirmDelete == null) deleteLabel else cancelLabel
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        TerminalUi.background(graphics, width, height)
        graphics.drawString(font, title, 16, 14, TerminalUi.TEXT, false)
        val message = error ?: if (loading) Component.translatable("ai_factory.shared.loading").string else
            Component.translatable("ai_factory.shared.counts", list?.sessions?.size ?: 0, list?.cells?.size ?: 0).string
        graphics.drawString(font, font.plainSubstrByWidth(message, width - 32), 16, 34,
            if (error != null) TerminalUi.ERROR else TerminalUi.MUTED, false)
        graphics.drawString(font, Component.translatable("ai_factory.shared.new_conversation"), sidebarX, 54, TerminalUi.ACCENT, false)
        val cell = list?.cells?.getOrNull(cellIndex)
        val usage = if (cell == null) "" else "${cell.usedBytes}/${cell.bytes} B · ${cell.sessionCount}/8"
        if (confirmDelete == null) graphics.drawString(font, font.plainSubstrByWidth(usage, 132), sidebarX, 145, TerminalUi.MUTED, false)
        val confirmation = confirmDelete
        if (confirmation != null) {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("ai_factory.shared.delete_warning", confirmation.title).string, 132),
                sidebarX, height - 92, TerminalUi.ERROR, false)
        }
        super.render(graphics, mouseX, mouseY, partialTick)
        if (error != null && mouseY in 30..46) graphics.renderTooltip(font, font.split(Component.literal(message), width - 40), mouseX, mouseY)
        if (confirmation != null && mouseX >= sidebarX && mouseY in (height - 96)..(height - 80)) {
            graphics.renderTooltip(font, font.split(Component.translatable("ai_factory.shared.delete_warning", confirmation.title), 220), mouseX, mouseY)
        }
    }

    private fun request() {
        if (pendingAction) return
        loading = true
        error = null
        confirmDelete = null
        refreshButtons()
        ClientSharedSessionRuntime.list(terminal) { result, failure ->
            if (Minecraft.getInstance().screen !== this) return@list
            loading = false
            error = failure
            if (result != null) list = result
            rebuildWidgets()
        }
    }

    private fun createSession(cell: SharedCellInfo?) {
        if (cell == null || pendingAction || loading) return
        pendingAction = true
        error = null
        refreshButtons()
        ClientSharedSessionRuntime.create(terminal, cell.id, titleInput.value) { info, failure ->
            pendingAction = false
            if (!acceptCallback(failure)) return@create
            if (info != null) openChat()
        }
    }

    private fun openSession(session: SharedSessionInfo) {
        if (pendingAction || loading || confirmDelete != null) return
        pendingAction = true
        error = null
        refreshButtons()
        ClientSharedSessionRuntime.open(terminal, session) { failure ->
            pendingAction = false
            if (acceptCallback(failure)) openChat()
        }
    }

    private fun acceptCallback(failure: String?): Boolean {
        if (Minecraft.getInstance().screen !== this) {
            if (failure == null && Minecraft.getInstance().screen !is SharedSessionSelectionScreen) ClientSharedSessionRuntime.onScreenClosed()
            return false
        }
        error = failure
        refreshButtons()
        return failure == null
    }

    private fun deleteSession(session: SharedSessionInfo) {
        if (pendingAction || loading) return
        pendingAction = true
        error = null
        refreshButtons()
        ClientSharedSessionRuntime.delete(terminal, session) { failure ->
            pendingAction = false
            if (Minecraft.getInstance().screen !== this) return@delete
            confirmDelete = null
            if (failure != null) { error = failure; refreshButtons() } else {
                selectedId = null
                request()
            }
        }
    }

    private fun openChat() {
        requested = false // Re-entering the browser fetches fresh titles and storage usage.
        openingChat = true // Internal navigation must not briefly release an idle lease.
        Minecraft.getInstance().setScreen(AgentChatScreen(ClientSharedSessionRuntime,
            Component.translatable("ai_factory.shared.chat_title"), this))
    }

    private fun cellLabel(cell: SharedCellInfo?): String = if (cell == null)
        Component.translatable("ai_factory.shared.no_cell").string else
        Component.translatable("ai_factory.shared.cell", cell.id.toString().take(8)).string
}
