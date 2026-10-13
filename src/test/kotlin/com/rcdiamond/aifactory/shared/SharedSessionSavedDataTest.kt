package com.rcdiamond.aifactory.shared

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.LLMContext
import java.util.UUID
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Exercises the world-save envelope the way a level reload does: `save(CompoundTag)` then `load`.
 *
 * The NBT layer is a single byte-array tag, so the interesting behaviour (what survives, what is charged,
 * what hash a reloaded session reports) all comes from [SharedSessionStore]; this test pins the envelope so
 * a change to the document format cannot silently break a save that already exists on disk.
 */
class SharedSessionSavedDataTest {

    private fun context(size: Int) = SharedContextCodec.encode(
        LLMContext(systemPrompt = "x".repeat(size), messages = listOf(ChatMessage.User("q"))),
    )

    @Test
    fun `an nbt save and reload returns the same sessions and their charges`() {
        val cell = UUID.randomUUID()
        val store = SharedSessionStore()
        val record = store.create(cell, "chain", UUID.randomUUID(), context(200))
        val data = SharedSessionSavedData(store)

        val reloaded = SharedSessionSavedData.load(data.save(CompoundTag()))

        assertEquals(record, reloaded.store.find(record.id))
        assertEquals(store.usedBytes(cell), reloaded.store.usedBytes(cell))
        assertEquals(1, reloaded.store.sessionCount(cell))
    }

}
