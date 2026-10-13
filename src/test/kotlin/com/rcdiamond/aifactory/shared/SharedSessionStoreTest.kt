package com.rcdiamond.aifactory.shared

import com.rcdiamond.aifactory.client.llm.ChatMessage
import com.rcdiamond.aifactory.client.llm.LLMContext
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedSessionStoreTest {

    private val cellA: UUID = UUID.randomUUID()
    private val cellB: UUID = UUID.randomUUID()

    private fun context(size: Int): JsonObject =
        SharedContextCodec.encode(LLMContext(systemPrompt = "x".repeat(size), messages = listOf(ChatMessage.User("q"))))

    private fun sessionJson(record: SharedSessionRecord): JsonObject = buildJsonObject {
        put("id", record.id.toString())
        put("cellId", record.cellId.toString())
        put("title", record.title)
        put("context", record.context)
    }

    @Test
    fun `a cell holds at most eight sessions for every tier`() {
        val store = SharedSessionStore()
        repeat(SharedSessionLimits.MAX_SESSIONS_PER_CELL) { index ->
            store.create(cellA, "session $index")
        }
        assertEquals(8, store.sessionCount(cellA))
        val rejected = assertFailsWith<IllegalArgumentException> { store.create(cellA, "one too many") }
        assertTrue(rejected.message!!.contains("maximum"))
        assertEquals(8, store.sessionCount(cellA))

        // A second cell is unaffected by the first one being full.
        store.create(cellB, "elsewhere")
        assertEquals(1, store.sessionCount(cellB))
    }

    @Test
    fun `used bytes are the ceiling of the sessions' tokens`() {
        val store = SharedSessionStore()
        val small = store.create(cellA, "small", UUID.randomUUID(), context(10))
        val large = store.create(cellA, "large", UUID.randomUUID(), context(1000))

        assertEquals(SharedSessionLimits.chargeFor(small.tokens) + SharedSessionLimits.chargeFor(large.tokens), store.usedBytes(cellA))
        assertEquals(0L, store.usedBytes(cellB))
    }

    @Test
    fun `a session's stored body is canonical and its hash survives a reordered rebuild`() {
        val store = SharedSessionStore()
        val record = store.create(cellA, "t", UUID.randomUUID(), context(50))

        assertEquals(record.context, SharedContextCodec.encode(SharedContextCodec.decode(record.context)))
        assertEquals(SharedContextCodec.hash(record.context), record.hash)
        assertEquals(SharedContextCodec.tokens(record.context), record.tokens)

        // A client that builds the same object with its keys in another order must still agree on the hash.
        val reordered = JsonObject(record.context.entries.reversed().associate { it.key to it.value })
        assertEquals(record.hash, SharedContextCodec.hash(reordered))
    }

    @Test
    fun `sessions and their bookkeeping survive an encode and decode cycle`() {
        val store = SharedSessionStore()
        val first = store.create(cellA, "first", UUID.randomUUID(), context(100))
        val second = store.create(cellB, "second", UUID.randomUUID(), context(5000))

        val reloaded = SharedSessionStore.decode(store.encode())

        assertEquals(store.all(), reloaded.all())
        assertEquals(first.hash, reloaded.find(first.id)!!.hash)
        assertEquals(first.tokens, reloaded.find(first.id)!!.tokens)
        assertEquals(second.charge, reloaded.find(second.id)!!.charge)
        assertEquals(store.usedBytes(cellA), reloaded.usedBytes(cellA))
        assertEquals(store.sessionCount(cellB), reloaded.sessionCount(cellB))
    }

    @Test
    fun `list and isMountedIn filter by the mounted cells only`() {
        val store = SharedSessionStore()
        val inA = store.create(cellA, "a", UUID.randomUUID(), context(10))
        store.create(cellB, "b", UUID.randomUUID(), context(10))

        assertEquals(listOf(inA), store.list(setOf(cellA)))
        assertTrue(store.isMountedIn(inA.id, setOf(cellA)))
        assertFalse(store.isMountedIn(inA.id, setOf(cellB)))
        assertFalse(store.isMountedIn(UUID.randomUUID(), setOf(cellA, cellB)))
    }

    @Test
    fun `replace canonicalises, recharges and refreshes the hash`() {
        val store = SharedSessionStore()
        val record = store.create(cellA, "t", UUID.randomUUID(), context(10))
        val before = store.usedBytes(cellA)

        val saved = store.replace(record.id, context(2000), SharedSessionLimits.CELL_1K_BYTES)
        assertEquals(record.id, saved.id)
        assertEquals(record.cellId, saved.cellId)
        assertEquals(record.title, saved.title)
        assertTrue(saved.tokens > record.tokens)
        assertEquals(before - record.charge + saved.charge, store.usedBytes(cellA))

        assertFailsWith<IllegalArgumentException> {
            store.replace(UUID.randomUUID(), context(1), SharedSessionLimits.CELL_1K_BYTES)
        }
    }

    @Test
    fun `a rejected replacement preserves the context and cell usage`() {
        val store = SharedSessionStore()
        val original = store.create(cellA, "t", UUID.randomUUID(), context(10))
        val budget = store.usedBytes(cellA)
        assertFailsWith<IllegalArgumentException> {
            store.replace(original.id, context(1000), budget)
        }
        assertEquals(original, store.find(original.id))
        assertEquals(budget, store.usedBytes(cellA))
    }

    @Test
    fun `the optimistic guard rejects a stale hash`() {
        val store = SharedSessionStore()
        val record = store.create(cellA, "t", UUID.randomUUID(), context(10))
        val stale = record.hash

        val saved = store.replace(record.id, context(20), SharedSessionLimits.CELL_1K_BYTES)
        assertFailsWith<IllegalArgumentException> { store.requireFresh(record.id, stale) }
        assertEquals(saved.hash, store.requireFresh(record.id, saved.hash).hash)
        assertFailsWith<IllegalArgumentException> { store.requireFresh(UUID.randomUUID(), saved.hash) }
    }

    @Test
    fun `requireRoom charges the cell and excludes the session being replaced`() {
        val store = SharedSessionStore()
        val record = store.create(cellA, "t", UUID.randomUUID(), context(1000))
        val occupied = store.usedBytes(cellA)

        // Re-saving the same body fits: the session is excluded from the used total.
        store.requireRoom(cellA, occupied, record.id, record.tokens)
        // A second body of the same size does not fit a cell that only has room for the first.
        assertFailsWith<IllegalArgumentException> { store.requireRoom(cellA, occupied, null, record.tokens) }
        // One more body fits once the cell has room for another charge.
        store.requireRoom(cellA, occupied + record.charge, null, record.tokens)
    }

    @Test
    fun `a title must be present and bounded`() {
        val store = SharedSessionStore()
        assertFailsWith<IllegalArgumentException> { store.create(cellA, "   ") }
        assertFailsWith<IllegalArgumentException> { store.create(cellA, "t".repeat(SharedSessionLimits.MAX_TITLE_LENGTH + 1)) }
        assertEquals(SharedSessionLimits.MAX_TITLE_LENGTH, store.create(cellA, "t".repeat(SharedSessionLimits.MAX_TITLE_LENGTH)).title.length)
    }


    @Test
    fun `a damaged entry is skipped while the rest survive`() {
        val store = SharedSessionStore()
        val good = store.create(cellA, "good", UUID.randomUUID(), context(10))
        val document = buildJsonObject {
            put("version", SharedSessionStore.DOCUMENT_VERSION)
            put(
                "sessions",
                buildJsonArray {
                    add(buildJsonObject { put("id", "not-a-uuid") })
                    add(sessionJson(good))
                },
            )
        }.toString().toByteArray(Charsets.UTF_8)

        val reloaded = SharedSessionStore.decode(document)
        assertEquals(listOf(good), reloaded.all())
        assertNull(reloaded.find(UUID.randomUUID()))
        assertFailsWith<IllegalArgumentException> {
            reloaded.replace(good.id, context(100), SharedSessionLimits.CELL_1K_BYTES)
        }
        assertTrue(document.contentEquals(reloaded.encode()))
    }

    @Test
    fun `damaged storage cannot be overwritten by creating a conversation`() {
        val bytes = byteArrayOf(1, 2, 3)
        val store = SharedSessionStore.decode(bytes)
        assertFailsWith<IllegalArgumentException> { store.create(cellA, "new") }
        assertTrue(bytes.contentEquals(store.encode()))
    }
}
