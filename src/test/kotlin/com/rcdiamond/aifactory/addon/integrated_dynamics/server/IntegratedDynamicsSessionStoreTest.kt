package com.rcdiamond.aifactory.addon.integrated_dynamics.server

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.LLMContext
import com.rcdiamond.aifactory.shared.SharedContextCodec
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class IntegratedDynamicsSessionStoreTest {
    private val terminal = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val other = UUID.fromString("10000000-0000-0000-0000-000000000002")

    private fun conversation(message: String) = SharedContextCodec.encode(
        LLMContext(systemPrompt = "Inspect this ID network", messages = listOf(ChatMessage.User(message))),
    )

    @Test
    fun `a known session UUID never grants access through another terminal`() {
        val store = IntegratedDynamicsSessionStore()
        val record = store.create(terminal, "Network", false)
        assertNull(store.find(other, record.id))
        assertFailsWith<IllegalArgumentException> { store.replace(other, record.id, record.hash, conversation("Other")) }
        assertFailsWith<IllegalArgumentException> { store.delete(other, record.id, record.hash) }
        assertEquals(record, store.find(terminal, record.id))
    }

    @Test
    fun `saved conversations and generated titles survive reload and stale edits cannot overwrite them`() {
        val store = IntegratedDynamicsSessionStore()
        val initial = store.create(terminal, "新会话", true)
        val saved = store.replace(terminal, initial.id, initial.hash, conversation("How many readers?"), "读取器检查")
        assertFalse(saved.autoTitle)
        val reloaded = IntegratedDynamicsSessionStore.decode(store.encode())
        assertEquals(saved, reloaded.find(terminal, initial.id))
        assertFailsWith<IllegalArgumentException> { reloaded.replace(terminal, initial.id, initial.hash, conversation("Stale")) }
        assertFailsWith<IllegalArgumentException> { reloaded.delete(terminal, initial.id, initial.hash) }
        assertFailsWith<IllegalArgumentException> { reloaded.replace(terminal, initial.id, saved.hash, saved.context, "Rename again") }
        assertEquals(saved, reloaded.find(terminal, initial.id))
        reloaded.delete(terminal, initial.id, saved.hash)
        assertNull(IntegratedDynamicsSessionStore.decode(reloaded.encode()).find(terminal, initial.id))
    }

    @Test
    fun `a full terminal does not consume another terminal's session slots and deletion frees a slot`() {
        val store = IntegratedDynamicsSessionStore()
        val first = store.create(terminal, "first", false)
        repeat(IntegratedDynamicsSessionStore.MAX_SESSIONS_PER_TERMINAL - 1) { store.create(terminal, "session $it", false) }
        assertFailsWith<IllegalArgumentException> { store.create(terminal, "overflow", false) }
        val elsewhere = store.create(other, "other network", false)
        assertEquals(elsewhere, store.find(other, elsewhere.id))
        store.delete(terminal, first.id, first.hash)
        val replacement = store.create(terminal, "replacement", false)
        assertEquals(replacement, store.find(terminal, replacement.id))
        assertNull(store.find(terminal, first.id))
    }

    @Test
    fun `rejected conversation or title leaves both content and automatic title state untouched`() {
        val store = IntegratedDynamicsSessionStore()
        val initial = store.create(terminal, "Untitled", true)
        val malformed = JsonObject(mapOf("systemPrompt" to JsonPrimitive("Missing messages")))
        assertFailsWith<IllegalArgumentException> { store.replace(terminal, initial.id, initial.hash, malformed, "Generated") }
        assertFailsWith<IllegalArgumentException> { store.replace(terminal, initial.id, initial.hash, conversation("valid"), " ") }
        val oversized = conversation("x".repeat(IntegratedDynamicsSessionStore.MAX_CONTEXT_BYTES + 1))
        assertFailsWith<IllegalArgumentException> { store.replace(terminal, initial.id, initial.hash, oversized, "Generated") }
        assertEquals(initial, store.find(terminal, initial.id))
    }

    @Test
    fun `damaged or newer world data is preserved rather than overwritten with empty sessions`() {
        for (document in listOf("{", "{\"version\":2,\"sessions\":[]}", "{\"version\":1,\"sessions\":[{}]}")) {
            val bytes = document.toByteArray()
            val store = IntegratedDynamicsSessionStore.decode(bytes)
            assertContentEquals(bytes, store.encode())
            assertFailsWith<IllegalArgumentException> { store.create(terminal, "New", false) }
            assertFailsWith<IllegalArgumentException> { store.find(terminal, UUID.randomUUID()) }
            assertContentEquals(bytes, store.encode())
        }
    }
}
