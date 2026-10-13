package com.rcdiamond.aifactory.client.shared

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

/**
 * Background indicator for a shared session that is open while its chat screen is not.
 *
 * It exists so a turn that keeps running after the screen is closed, and a conversation that could not be
 * saved yet, stay visible instead of silently continuing. Nothing here is interactive and nothing is read
 * from the server: it only shows what [ClientSharedSessionRuntime] already knows on the client thread.
 */
object SharedSessionHud : IGuiOverlay {
    private const val PANEL_WIDTH = 220
    private const val MARGIN = 6
    private const val LINE_SPACING = 2
    private const val BACKGROUND = 0x90000000.toInt()
    private const val TITLE_COLOR = 0xFFD479
    private const val TEXT_COLOR = 0xE0E0E0

    override fun render(gui: ForgeGui, graphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        if (!ClientSharedSessionRuntime.hudVisible()) return
        val minecraft = Minecraft.getInstance()
        if (minecraft.options.hideGui) return
        val font = minecraft.font

        val lines = ArrayList<String>(3)
        lines += Component.translatable(
            "ai_factory.shared.hud_title",
            ClientSharedSessionRuntime.hudTitle(),
            ClientSharedSessionRuntime.hudTokens(),
        ).string
        val status = ClientSharedSessionRuntime.status
        if (status.isNotBlank()) lines += status
        if (ClientSharedSessionRuntime.hudRunning()) {
            lines += Component.translatable("ai_factory.shared.hud_running").string
        }
        val tail = ClientSharedSessionRuntime.hudTail()
        if (!tail.isNullOrBlank()) lines += tail

        val lineHeight = font.lineHeight + LINE_SPACING
        val panelHeight = lines.size * lineHeight + 6
        graphics.fill(MARGIN, MARGIN, MARGIN + PANEL_WIDTH + 8, MARGIN + panelHeight, BACKGROUND)
        var y = MARGIN + 4
        for ((index, line) in lines.withIndex()) {
            val color = if (index == 0) TITLE_COLOR else TEXT_COLOR
            graphics.drawString(font, font.plainSubstrByWidth(line, PANEL_WIDTH), MARGIN + 4, y, color)
            y += lineHeight
        }
    }
}
