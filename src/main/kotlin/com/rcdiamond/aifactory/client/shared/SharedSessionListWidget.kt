package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.client.llm.SharedSessionInfo
import com.rcdiamond.aifactory.client.ui.TerminalUi
import net.minecraft.Util
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.util.UUID
import kotlin.math.ceil

/** Virtualized rows, not a bank of buttons. Selection survives filtering and server refreshes. */
class SharedSessionListWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val font: Font,
    private val onSelection: (SharedSessionInfo?) -> Unit,
    private val onOpen: (SharedSessionInfo) -> Unit,
) : AbstractWidget(x, y, width, height, Component.translatable("ai_factory.shared.conversations")) {
    private var sessions: List<SharedSessionInfo> = emptyList()
    private class Row(val title: String, val detail: String)
    private var rows: List<Row> = emptyList()
    private val emptyLabel = Component.translatable("ai_factory.shared.empty_list")
    private var selectedId: UUID? = null
    private var scroll = 0
    private var lastClick = 0L
    private var lastClickedId: UUID? = null
    val selected: SharedSessionInfo? get() = sessions.firstOrNull { it.id == selectedId }
    private val visibleRows get() = (height / ROW_HEIGHT).coerceAtLeast(1)
    private val maxScroll get() = (sessions.size - visibleRows).coerceAtLeast(0)

    fun update(values: List<SharedSessionInfo>, preferred: UUID? = selectedId) {
        sessions = values
        rows = values.map { entry ->
            Row(font.plainSubstrByWidth(entry.title, width - 22),
                font.plainSubstrByWidth(Component.translatable("ai_factory.shared.row_details",
                    entry.tokens, entry.cellId.toString().take(8)).string, width - 22))
        }
        selectedId = preferred?.takeIf { id -> values.any { it.id == id } }
        scroll = scroll.coerceIn(0, maxScroll)
        onSelection(selected)
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(x, y, x + width, y + height, TerminalUi.PANEL)
        graphics.enableScissor(x, y, x + width, y + height)
        if (sessions.isEmpty()) {
            graphics.drawString(font, emptyLabel, x + 10, y + 12, TerminalUi.MUTED, false)
        }
        val end = (scroll + visibleRows).coerceAtMost(sessions.size)
        for (index in scroll until end) {
            val entry = sessions[index]
            val top = y + (index - scroll) * ROW_HEIGHT
            val selected = entry.id == selectedId
            val hovered = mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < top + ROW_HEIGHT
            if (selected || hovered) graphics.fill(x, top, x + width, top + ROW_HEIGHT, if (selected) TerminalUi.SELECTED else TerminalUi.HOVER)
            if (selected) graphics.fill(x, top + 3, x + 2, top + ROW_HEIGHT - 3, TerminalUi.ACCENT)
            val row = rows[index]
            graphics.drawString(font, row.title, x + 10, top + 6, TerminalUi.TEXT, false)
            graphics.drawString(font, row.detail, x + 10, top + 19, TerminalUi.MUTED, false)
            graphics.fill(x + 8, top + ROW_HEIGHT - 1, x + width - 8, top + ROW_HEIGHT, TerminalUi.BORDER)
        }
        if (maxScroll > 0) {
            val thumbHeight = (height * visibleRows / sessions.size).coerceAtLeast(12).coerceAtMost(height)
            val top = y + (height - thumbHeight) * scroll / maxScroll
            graphics.fill(x + width - 3, top, x + width - 1, top + thumbHeight, TerminalUi.ACCENT)
        }
        if (isFocused) graphics.fill(x, y, x + width, y + 1, TerminalUi.ACCENT)
        graphics.disableScissor()
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        val index = scroll + (mouseY.toInt() - y) / ROW_HEIGHT
        val entry = sessions.getOrNull(index) ?: return
        val now = Util.getMillis()
        val doubleClick = lastClickedId == entry.id && now - lastClick < 300L
        selectedId = entry.id
        onSelection(entry)
        lastClickedId = entry.id
        lastClick = now
        if (doubleClick) onOpen(entry)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (!active || !isMouseOver(mouseX, mouseY)) return false
        scroll = (scroll - ceil(delta * 2).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!active || !isFocused) return false
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            selected?.let(onOpen)
            return true
        }
        val direction = when (keyCode) {
            GLFW.GLFW_KEY_UP -> -1
            GLFW.GLFW_KEY_DOWN -> 1
            GLFW.GLFW_KEY_HOME -> -sessions.size
            GLFW.GLFW_KEY_END -> sessions.size
            else -> return super.keyPressed(keyCode, scanCode, modifiers)
        }
        if (sessions.isEmpty()) return true
        val index = sessions.indexOfFirst { it.id == selectedId }
        val next = when {
            keyCode == GLFW.GLFW_KEY_END -> sessions.lastIndex
            keyCode == GLFW.GLFW_KEY_HOME -> 0
            index < 0 -> 0
            else -> (index + direction).coerceIn(0, sessions.lastIndex)
        }
        selectedId = sessions[next].id
        if (next < scroll) scroll = next
        if (next >= scroll + visibleRows) scroll = next - visibleRows + 1
        onSelection(sessions[next])
        return true
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }

    companion object { const val ROW_HEIGHT = 34 }
}
