package com.rcdiamond.aifactory.client.shared

import com.rcdiamond.aifactory.addon.integrated_dynamics.client.IntegratedDynamicsAgentRuntime
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
 * from the server: it shows each addon's independent client runtime.
 */
object SharedSessionHud : IGuiOverlay {
    private const val PANEL_WIDTH = 220
    private const val MARGIN = 6
    private const val LINE_SPACING = 2
    private const val BACKGROUND = 0x90000000.toInt()
    private const val TITLE_COLOR = 0xFFD479
    private const val TEXT_COLOR = 0xE0E0E0

    override fun render(gui: ForgeGui, graphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        if (Minecraft.getInstance().options.hideGui) return
        val nextY = renderSession(graphics, ClientSharedSessionRuntime, MARGIN)
        renderSession(graphics, IntegratedDynamicsAgentRuntime, nextY)
    }

    private fun renderSession(graphics: GuiGraphics, runtime: SharedAgentSessionRuntime, top: Int): Int {
        if (!runtime.hudVisible()) return top
        val font = Minecraft.getInstance().font
        val lines = ArrayList<String>(3)
        lines += Component.translatable(
            if (runtime.profile.usesCells) "ai_factory.shared.hud_title" else "ai_factory.id.hud_title",
            runtime.hudTitle(),
            runtime.hudTokens(),
        ).string
        val status = runtime.status
        if (status.isNotBlank()) lines += status
        if (runtime.hudRunning()) lines += Component.translatable("ai_factory.shared.hud_running").string
        val tail = runtime.hudTail()
        if (!tail.isNullOrBlank()) lines += tail

        val lineHeight = font.lineHeight + LINE_SPACING
        val panelHeight = lines.size * lineHeight + 6
        graphics.fill(MARGIN, top, MARGIN + PANEL_WIDTH + 8, top + panelHeight, BACKGROUND)
        var y = top + 4
        for ((index, line) in lines.withIndex()) {
            val color = if (index == 0) TITLE_COLOR else TEXT_COLOR
            graphics.drawString(font, font.plainSubstrByWidth(line, PANEL_WIDTH), MARGIN + 4, y, color)
            y += lineHeight
        }
        return top + panelHeight + MARGIN
    }
}
