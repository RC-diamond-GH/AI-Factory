package com.rcdiamond.aifactory.shared

import java.util.UUID
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData

/**
 * The world-save envelope around a [SharedSessionStore].
 *
 * The whole store travels as one UTF-8 document inside a single byte-array tag, so a large conversation
 * (tens of megabytes for a full 256Ki cell) is not limited by NBT string tags. The NBT layer owns no logic
 * of its own: [SharedSessionStore.encode]/[decode] do the real work and are unit-tested on their own.
 *
 * The data lives on the overworld's data storage, not per dimension, so a cell keeps its sessions when its
 * drive crosses dimensions.
 *
 * ## Fail-closed limits
 * A damaged or unsupported document is preserved read-only instead of being overwritten with empty
 * data; readable records remain visible, but edits require restoring a backup. Leases are not recovered.
 * A crash between client `save` and the next level flush can lose the acknowledged-but-not-flushed
 * context. Extreme disconnect/unload robustness is outside this implementation's scope.
 */
class SharedSessionSavedData internal constructor(val store: SharedSessionStore) : SavedData() {
    override fun save(tag: CompoundTag): CompoundTag {
        tag.putByteArray(DATA_KEY, store.encode())
        return tag
    }

    /** Marks the chunk for the next level save after a mutation. */
    fun markChanged() {
        setDirty()
    }

    companion object {
        /** Name under which the document is stored on the overworld's data storage. */
        const val DATA_NAME: String = "ai_factory_shared_sessions"

        private const val DATA_KEY = "sessions"

        /** Reads a document written by [save] back into a store. Entries that cannot be read are skipped. */
        internal fun load(tag: CompoundTag): SharedSessionSavedData =
            SharedSessionSavedData(SharedSessionStore.decode(tag.getByteArray(DATA_KEY)))

        /** The store of [server], created on first use and cached by the level's data storage. */
        fun get(server: MinecraftServer): SharedSessionSavedData =
            server.overworld().dataStorage.computeIfAbsent(
                { tag -> load(tag) },
                { SharedSessionSavedData(SharedSessionStore()) },
                DATA_NAME,
            )
    }
}

/**
 * Read-only view the addon uses to report a cell's real occupancy (drive status, terminal screen) without
 * going through the RPC layer. Both values come straight from the world-save store, so an AE drive can
 * never display usage that does not correspond to stored sessions.
 */
object SharedCellStore {
    /** ME bytes charged by the sessions bound to [cellId]. */
    fun usedBytes(level: ServerLevel, cellId: UUID): Long =
        SharedSessionSavedData.get(level.server).store.usedBytes(cellId)

    /** Number of sessions bound to [cellId]. */
    fun sessionCount(level: ServerLevel, cellId: UUID): Int =
        SharedSessionSavedData.get(level.server).store.sessionCount(cellId)
}
