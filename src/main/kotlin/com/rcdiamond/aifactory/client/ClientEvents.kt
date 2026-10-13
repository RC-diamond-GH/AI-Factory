package com.rcdiamond.aifactory.client

import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.client.game.ClientGameActions
import com.rcdiamond.aifactory.client.ui.AgentChatScreen
import com.rcdiamond.aifactory.network.ExecuteCommandResponseEvent
import com.rcdiamond.aifactory.network.GiveItemResponseEvent
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
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
    fun disconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientAgentRuntime.disconnect()
    }
}
