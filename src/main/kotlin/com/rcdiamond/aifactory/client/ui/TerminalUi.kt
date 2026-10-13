package com.rcdiamond.aifactory.client.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

/** Opaque, flat surfaces: no vanilla dimming gradient or textured controls. */
object TerminalUi {
    const val BACKGROUND = 0xFF101820.toInt()
    const val PANEL = 0xFF18242F.toInt()
    const val HOVER = 0xFF233543.toInt()
    const val SELECTED = 0xFF264653.toInt()
    const val BORDER = 0xFF344956.toInt()
    const val ACCENT = 0xFF65D6C2.toInt()
    const val TEXT = 0xFFE4EDF2.toInt()
    const val MUTED = 0xFF9BAFBD.toInt()
    const val ERROR = 0xFFFF9292.toInt()

    fun background(graphics: GuiGraphics, width: Int, height: Int) {
        graphics.fill(0, 0, width, height, BACKGROUND)
        graphics.fill(0, 0, 3, height, ACCENT)
    }
}

class FlatButton(
    x: Int, y: Int, width: Int, height: Int,
    message: Component,
    onPress: OnPress,
    private val destructive: Boolean = false,
) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
    private var cachedMessage: Component? = null
    private var cachedWidth = -1
    private var text = ""
    private var textWidth = 0
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val highlight = active && (isHovered || isFocused)
        graphics.fill(x, y, x + width, y + height, if (highlight) TerminalUi.HOVER else TerminalUi.PANEL)
        graphics.fill(x, y + height - 1, x + width, y + height,
            if (highlight) { if (destructive) TerminalUi.ERROR else TerminalUi.ACCENT } else TerminalUi.BORDER)
        val font = Minecraft.getInstance().font
        if (cachedMessage !== message || cachedWidth != width) {
            text = font.plainSubstrByWidth(message.string, (width - 8).coerceAtLeast(0))
            textWidth = font.width(text)
            cachedMessage = message
            cachedWidth = width
        }
        val color = if (!active) TerminalUi.MUTED else if (destructive) TerminalUi.ERROR else TerminalUi.TEXT
        graphics.drawString(font, text, x + (width - textWidth) / 2, y + (height - 8) / 2, color, false)
    }
}

/** Keeps vanilla editing, selection and clipboard handling, with a flat inset surface. */
class FlatEditBox(font: Font, x: Int, y: Int, width: Int, height: Int, message: Component) :
    EditBox(font, x + 6, y + 6, width - 12, height - 12, message) {
    init {
        setBordered(false)
        setTextColor(TerminalUi.TEXT)
        setTextColorUneditable(TerminalUi.MUTED)
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(x - 6, y - 6, x + width + 6, y + height + 6, TerminalUi.PANEL)
        graphics.fill(x - 6, y + height + 5, x + width + 6, y + height + 6,
            if (isFocused) TerminalUi.ACCENT else TerminalUi.BORDER)
        super.renderWidget(graphics, mouseX, mouseY, partialTick)
    }
}
