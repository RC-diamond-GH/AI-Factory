package com.rcdiamond.aifactory.client.ui

import com.rcdiamond.aifactory.client.llm.AgentChatSession
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil

class AgentChatScreen(
    private val session: AgentChatSession,
    title: Component = Component.translatable("ai_factory.chat.title"),
    private val returnScreen: Screen? = null,
) : Screen(title) {
    private lateinit var input: EditBox
    private lateinit var send: FlatButton
    private lateinit var cancel: FlatButton
    private lateinit var clear: FlatButton
    private lateinit var reload: FlatButton
    private lateinit var latest: FlatButton
    private var confirmClear = false
    private var returningToList = false
    private val clearLabel = Component.translatable("ai_factory.chat.clear")
    private val confirmClearLabel = Component.translatable("ai_factory.chat.confirm_clear")
    private var cachedRevision = -1L
    private var cachedWidth = -1
    private data class Line(val text: FormattedCharSequence?, val color: Int, val surface: Int)
    private val lines = ArrayList<Line>()
    private var scrollLines = 0
    private val lineHeight get() = font.lineHeight + 4
    private val left get() = ((width - 800) / 2).coerceAtLeast(16)
    private val right get() = width - left
    private val bodyTop = 72
    private val bodyBottom get() = height - 70
    private val visibleLines get() = ((bodyBottom - bodyTop - 8) / lineHeight).coerceAtLeast(1)

    override fun init() {
        returningToList = false
        val draft = if (::input.isInitialized) input.value else ""
        val inputWidth = (right - left - 140).coerceAtLeast(40)
        input = FlatEditBox(font, left, height - 32, inputWidth, 20, Component.translatable("ai_factory.chat.input"))
        input.setMaxLength(4000)
        input.setHint(Component.translatable(if (returnScreen == null) "ai_factory.chat.hint" else "ai_factory.shared.chat_hint"))
        input.value = draft
        addRenderableWidget(input)
        send = button(right - 132, height - 32, 64, "ai_factory.chat.send") { submit() }
        cancel = button(right - 64, height - 32, 64, "ai_factory.chat.cancel") { session.cancelTurn() }
        if (returnScreen != null) button(left, 12, 56, "ai_factory.shared.back") { onClose() }
        clear = button(right - 166, 38, 78, "ai_factory.chat.clear") {
            if (confirmClear) { session.clear(); confirmClear = false } else confirmClear = true
            refreshButtons()
        }
        reload = button(right - 82, 38, 82, "ai_factory.chat.reload") { confirmClear = false; session.reload() }
        addRenderableWidget(FlatButton(right - 26, 12, 26, 20, Component.literal("×"), { minecraft?.setScreen(null) }))
        latest = button(right - 100, height - 62, 100, "ai_factory.chat.latest") { scrollLines = 0; refreshButtons() }
        setInitialFocus(input)
        if (session.status.isEmpty()) session.reload()
        refreshButtons()
    }

    private fun button(x: Int, y: Int, width: Int, key: String, action: () -> Unit): FlatButton =
        addRenderableWidget(FlatButton(x, y, width, 20, Component.translatable(key), { action() }))

    override fun tick() {
        input.tick()
        refreshButtons()
    }

    private fun refreshButtons() {
        val busy = session.busy
        send.active = !busy && input.value.isNotBlank()
        cancel.active = busy
        clear.active = !busy
        reload.active = !busy
        clear.message = if (confirmClear) confirmClearLabel else clearLabel
        latest.visible = scrollLines > 0
        // Drafting remains available while a turn runs; only sending is blocked.
        input.setEditable(true)
    }

    private fun submit() {
        val message = input.value.trim()
        if (message.isEmpty() || session.busy) return
        input.value = ""
        confirmClear = false
        scrollLines = 0
        session.submit(message)
        refreshButtons()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && input.isFocused) {
            submit()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (mouseX < left || mouseX >= right || mouseY < bodyTop || mouseY >= bodyBottom) return super.mouseScrolled(mouseX, mouseY, delta)
        rebuildLines()
        scrollLines = (scrollLines + ceil(delta * 3).toInt()).coerceIn(0, (lines.size - visibleLines).coerceAtLeast(0))
        refreshButtons()
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        TerminalUi.background(graphics, width, height)
        val titleX = if (returnScreen == null) left else left + 64
        graphics.drawString(font, font.plainSubstrByWidth(session.conversationTitle ?: title.string,
            (right - 34 - titleX).coerceAtLeast(12)), titleX, 18, TerminalUi.TEXT, false)
        graphics.fill(left, 44, left + 4, 52, if (session.busy) TerminalUi.ACCENT else TerminalUi.MUTED)
        graphics.drawString(font, font.plainSubstrByWidth(session.status, (right - left - 186).coerceAtLeast(12)), left + 12, 44, TerminalUi.MUTED, false)
        rebuildLines()
        graphics.fill(left, bodyTop, right, bodyBottom, TerminalUi.PANEL)
        graphics.enableScissor(left, bodyTop, right, bodyBottom)
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("ai_factory.chat.empty"), left + 12, bodyTop + 14, TerminalUi.ACCENT, false)
            val help = font.split(Component.translatable(if (returnScreen == null) "ai_factory.chat.empty_hint" else "ai_factory.shared.empty_hint"), right - left - 28)
            for (index in help.indices) graphics.drawString(font, help[index], left + 12, bodyTop + 34 + index * lineHeight, TerminalUi.MUTED, false)
        }
        val start = (lines.size - visibleLines - scrollLines).coerceAtLeast(0)
        val end = (start + visibleLines).coerceAtMost(lines.size)
        for (index in start until end) {
            val line = lines[index]
            val y = bodyTop + 4 + (index - start) * lineHeight
            graphics.fill(left + 4, y, right - 6, y + lineHeight, line.surface)
            line.text?.let { graphics.drawString(font, it, left + 12, y + 2, line.color, false) }
        }
        if (lines.size > visibleLines) {
            val track = bodyBottom - bodyTop
            val thumb = (track * visibleLines / lines.size).coerceAtLeast(10)
            val top = bodyTop + (track - thumb) * start / (lines.size - visibleLines)
            graphics.fill(right - 3, top, right - 1, top + thumb, TerminalUi.ACCENT)
        }
        graphics.disableScissor()
        graphics.drawString(font, Component.translatable(if (confirmClear) "ai_factory.chat.clear_warning" else "ai_factory.chat.composer_hint"),
            left, height - 48, if (confirmClear) TerminalUi.ERROR else TerminalUi.MUTED, false)
        refreshButtons()
        super.render(graphics, mouseX, mouseY, partialTick)
        if (mouseY in 40..56 && mouseX < right - 174) graphics.renderTooltip(font, font.split(Component.literal(session.status), right - left - 16), mouseX, mouseY)
    }

    private fun rebuildLines() {
        val wrapWidth = (right - left - 28).coerceAtLeast(20)
        if (cachedRevision == session.revision && cachedWidth == wrapWidth) return
        val oldSize = lines.size
        lines.clear()
        for (entry in session.entries) {
            val color = when (entry.speaker) {
                "user" -> TerminalUi.ACCENT
                "assistant" -> TerminalUi.TEXT
                "tool" -> TerminalUi.MUTED
                else -> TerminalUi.ERROR
            }
            val surface = if (entry.speaker == "user") TerminalUi.HOVER else TerminalUi.PANEL
            val speaker = Component.translatable("ai_factory.chat.speaker.${entry.speaker}")
            lines += Line(speaker.visualOrderText, color, surface)
            for (line in font.split(Component.literal(entry.text), wrapWidth)) lines += Line(line, TerminalUi.TEXT, surface)
            lines += Line(null, TerminalUi.MUTED, TerminalUi.PANEL)
        }
        if (scrollLines > 0) scrollLines += lines.size - oldSize
        scrollLines = scrollLines.coerceIn(0, (lines.size - visibleLines).coerceAtLeast(0))
        cachedRevision = session.revision
        cachedWidth = wrapWidth
    }

    override fun onClose() {
        returningToList = returnScreen != null && !session.cancelTurnOnClose
        minecraft?.setScreen(returnScreen)
    }

    override fun isPauseScreen(): Boolean = false

    override fun added() {
        super.added()
        session.onScreenOpened()
    }

    override fun removed() {
        if (session.cancelTurnOnClose) session.cancelTurn()
        if (!returningToList) session.onScreenClosed()
        super.removed()
    }
}
