package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

/** Separate world-save namespace from ME conversations; bodies are not limited by NBT string length. */
class IntegratedDynamicsSessionSavedData(val store: IntegratedDynamicsSessionStore) : SavedData() {
    override fun save(tag: CompoundTag): CompoundTag {
        tag.putByteArray("sessions", store.encode())
        return tag
    }

    companion object {
        const val DATA_NAME = "ai_factory_integrated_dynamics_sessions"

        fun get(server: MinecraftServer): IntegratedDynamicsSessionSavedData =
            server.overworld().dataStorage.computeIfAbsent(
                { tag -> IntegratedDynamicsSessionSavedData(IntegratedDynamicsSessionStore.decode(tag.getByteArray("sessions"))) },
                { IntegratedDynamicsSessionSavedData(IntegratedDynamicsSessionStore()) },
                DATA_NAME,
            )
    }
}
