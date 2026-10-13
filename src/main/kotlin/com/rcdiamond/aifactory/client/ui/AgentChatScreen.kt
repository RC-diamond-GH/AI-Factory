package com.rcdiamond.aifactory.client.ui

import com.rcdiamond.aifactory.client.llm.AgentChatSession
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil

class AgentChatScreen(
    private val session: AgentChatSession,
    title: Component = Component.translatable("ai_factory.chat.title"),
) : Screen(title) {
    private lateinit var input: EditBox
    private lateinit var send: Button
    private lateinit var cancel: Button
    private lateinit var clear: Button
    private lateinit var reload: Button
    private var cachedRevision = -1L
    private var cachedWidth = -1
    private val lines = ArrayList<FormattedCharSequence>()
    private var scrollLines = 0
    private val lineHeight get() = font.lineHeight + 2
    private val visibleLines get() = ((height - 104) / lineHeight).coerceAtLeast(1)

    override fun init() {
        val draft = if (::input.isInitialized) input.value else ""
        input = EditBox(font, 12, height - 30, (width - 164).coerceAtLeast(40), 20, Component.translatable("ai_factory.chat.input"))
        input.setMaxLength(4000)
        input.setHint(Component.translatable("ai_factory.chat.hint"))
        input.value = draft
        addRenderableWidget(input)
        send = addRenderableWidget(Button.builder(Component.translatable("ai_factory.chat.send")) { submit() }
            .bounds(width - 144, height - 30, 64, 20).build())
        cancel = addRenderableWidget(Button.builder(Component.translatable("ai_factory.chat.cancel")) { session.cancelTurn() }
            .bounds(width - 76, height - 30, 64, 20).build())
        clear = addRenderableWidget(Button.builder(Component.translatable("ai_factory.chat.clear")) { session.clear() }
            .bounds(width - 176, 10, 78, 20).build())
        reload = addRenderableWidget(Button.builder(Component.translatable("ai_factory.chat.reload")) { session.reload() }
            .bounds(width - 94, 10, 82, 20).build())
        setInitialFocus(input)
        if (session.status.isEmpty()) session.reload()
        refreshButtons()
    }

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
        input.setEditable(!busy)
    }

    private fun submit() {
        val message = input.value.trim()
        if (message.isEmpty() || session.busy) return
        input.value = ""
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
        scrollLines = (scrollLines + ceil(delta * 3).toInt()).coerceIn(0, (lines.size - visibleLines).coerceAtLeast(0))
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        graphics.drawString(font, title, 12, 16, 0xffffff)
        graphics.drawString(font, font.plainSubstrByWidth(session.status, width - 24), 12, 36, 0xb0b0b0)
        rebuildLines()
        graphics.fill(10, 50, width - 10, height - 42, 0x90000000.toInt())
        graphics.enableScissor(12, 52, width - 12, height - 44)
        val start = (lines.size - visibleLines - scrollLines).coerceAtLeast(0)
        val end = (start + visibleLines).coerceAtMost(lines.size)
        for (index in start until end) {
            graphics.drawString(font, lines[index], 14, 54 + (index - start) * lineHeight, 0xffffff)
        }
        graphics.disableScissor()
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun rebuildLines() {
        val wrapWidth = (width - 32).coerceAtLeast(20)
        if (cachedRevision == session.revision && cachedWidth == wrapWidth) return
        val oldSize = lines.size
        lines.clear()
        for (entry in session.entries) {
            val color = when (entry.speaker) {
                "user" -> ChatFormatting.GREEN
                "assistant" -> ChatFormatting.AQUA
                "tool" -> ChatFormatting.YELLOW
                else -> ChatFormatting.GRAY
            }
            val content = Component.translatable("ai_factory.chat.speaker.${entry.speaker}")
                .withStyle(color).append(Component.literal(": ${entry.text}").withStyle(ChatFormatting.WHITE))
            lines.addAll(font.split(content, wrapWidth))
        }
        if (scrollLines > 0) scrollLines += lines.size - oldSize
        scrollLines = scrollLines.coerceIn(0, (lines.size - visibleLines).coerceAtLeast(0))
        cachedRevision = session.revision
        cachedWidth = wrapWidth
    }

    override fun isPauseScreen(): Boolean = false

    override fun added() {
        super.added()
        session.onScreenOpened()
    }

    override fun removed() {
        // A shared session keeps its turn running while no screen is shown; only the local chat cancels.
        if (session.cancelTurnOnClose) session.cancelTurn()
        session.onScreenClosed()
        super.removed()
    }
}
