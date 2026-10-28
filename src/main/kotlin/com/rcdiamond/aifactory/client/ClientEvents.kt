package com.rcdiamond.aifactory.client

import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.IntegratedDynamicsAgentRuntime
import com.rcdiamond.aifactory.client.game.ClientGameActions
import com.rcdiamond.aifactory.client.shared.ClientSharedSessionRuntime
import com.rcdiamond.aifactory.client.shared.SharedSessionHud
import com.rcdiamond.aifactory.client.shared.SharedSessionRpc
import com.rcdiamond.aifactory.client.ui.AgentChatScreen
import com.rcdiamond.aifactory.network.ExecuteCommandResponseEvent
import com.rcdiamond.aifactory.network.GiveItemResponseEvent
import com.rcdiamond.aifactory.network.SharedSessionResponseEvent
import com.rcdiamond.aifactory.network.SharedTerminalOpenEvent
import kotlinx.serialization.json.JsonPrimitive
import net.minecraftforge.fml.ModList
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.lwjgl.glfw.GLFW

@Mod.EventBusSubscriber(modid = AIFactory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientModEvents {
    val openChat = KeyMapping("key.ai_factory.agent_chat", GLFW.GLFW_KEY_G, "key.categories.ai_factory")

    @SubscribeEvent
    fun registerKeys(event: RegisterKeyMappingsEvent) {
        event.register(openChat)
    }

    /** Background indicator for a shared session whose chat screen is not showing. */
    @SubscribeEvent
    fun registerOverlays(event: RegisterGuiOverlaysEvent) {
        event.registerAbove(VanillaGuiOverlay.CHAT_PANEL.id(), "shared_session_hud", SharedSessionHud)
    }
}

@Mod.EventBusSubscriber(modid = AIFactory.MODID, value = [Dist.CLIENT])
object ClientGameEvents {
    @SubscribeEvent
    fun clientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val minecraft = Minecraft.getInstance()
        while (ClientModEvents.openChat.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(AgentChatScreen(ClientAgentRuntime))
        }
    }

    @SubscribeEvent
    fun giveItemResponse(event: GiveItemResponseEvent) {
        ClientGameActions.complete(event.result)
    }

    @SubscribeEvent
    fun executeCommandResponse(event: ExecuteCommandResponseEvent) {
        ClientGameActions.complete(event.result)
    }

    @SubscribeEvent
    fun sharedSessionResponse(event: SharedSessionResponseEvent) {
        SharedSessionRpc.onResponse(event.requestId, event.payload)
    }

    @SubscribeEvent
    fun sharedTerminalOpen(event: SharedTerminalOpenEvent) {
        val kind = (event.terminal["kind"] as? JsonPrimitive)?.content
        when (kind) {
            null -> ClientSharedSessionRuntime.onTerminalOpen(event.terminal)
            "integrated_dynamics" -> if (ModList.get().isLoaded("integrateddynamics")) {
                IntegratedDynamicsAgentRuntime.onTerminalOpen(event.terminal)
            }
        }
    }

    @SubscribeEvent
    fun disconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientAgentRuntime.disconnect()
        ClientSharedSessionRuntime.disconnect()
        IntegratedDynamicsAgentRuntime.disconnect()
    }
}
