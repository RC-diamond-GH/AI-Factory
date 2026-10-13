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
    fun `cell titles retain document order and include sessions created after a cached read`() {
        val store = SharedSessionStore()
        assertEquals(emptyList(), store.titles(cellA))
        val first = store.create(cellA, "first")
        store.create(cellB, "other cell")
        assertEquals(listOf("first"), store.titles(cellA))
        store.replace(first.id, context(2000), 1024)
        store.create(cellA, "second")

        assertEquals(listOf("first", "second"), store.titles(cellA))
        assertEquals(listOf("other cell"), store.titles(cellB))
        assertEquals(store.titles(cellA), SharedSessionStore.decode(store.encode()).titles(cellA))
    }

    @Test
    fun `generated titles update the cached cell summary and survive reload without changing the context hash`() {
        val store = SharedSessionStore()
        val session = store.create(cellA, "新会话", context = context(10), autoTitle = true)
        assertEquals(listOf("新会话"), store.titles(cellA))
        val pending = SharedSessionStore.decode(store.encode()).find(session.id)
        assertTrue(pending?.autoTitle == true)

        val saved = store.replace(session.id, session.context, 1024, "铁锭合成")

        assertEquals(session.hash, saved.hash)
        assertEquals(session.tokens, saved.tokens)
        assertFalse(saved.autoTitle)
        assertEquals(listOf("铁锭合成"), store.titles(cellA))
        val reloaded = SharedSessionStore.decode(store.encode())
        assertEquals(saved, reloaded.find(session.id))
        assertEquals(listOf("铁锭合成"), reloaded.titles(cellA))
        assertFailsWith<IllegalArgumentException> {
            store.replace(session.id, session.context, 1024, "再次改名")
        }
        assertEquals(saved, store.find(session.id))
    }

    @Test
    fun `failed saves preserve the pending title flag and never publish a generated title`() {
        val store = SharedSessionStore()
        val session = store.create(cellA, "新会话", autoTitle = true)
        val budget = store.usedBytes(cellA)
        assertEquals(listOf("新会话"), store.titles(cellA))
        assertFailsWith<IllegalArgumentException> {
            store.replace(session.id, context(1000), budget, "铁锭合成")
        }
        for (title in listOf(" ", "t".repeat(SharedSessionLimits.MAX_TITLE_LENGTH + 1))) {
            assertFailsWith<IllegalArgumentException> {
                store.replace(session.id, session.context, 1024, title)
            }
        }
        assertEquals(session, store.find(session.id))
        assertEquals(listOf("新会话"), store.titles(cellA))
        assertEquals(budget, store.usedBytes(cellA))
    }

    @Test
    fun `manually named sessions including the placeholder cannot be automatically renamed`() {
        val store = SharedSessionStore()
        val session = store.create(cellA, "新会话")
        val reloaded = SharedSessionStore.decode(store.encode())
        assertFalse(reloaded.find(session.id)?.autoTitle ?: true)
        assertFailsWith<IllegalArgumentException> {
            reloaded.replace(session.id, session.context, 1024, "铁锭合成")
        }
        assertEquals(session, reloaded.find(session.id))
    }

    @Test
    fun `legacy placeholder titles become eligible while other legacy names are retained`() {
        val store = SharedSessionStore()
        val chinese = store.create(cellA, "新会话")
        val english = store.create(cellA, "New conversation")
        val manual = store.create(cellA, "My factory")
        val document = buildJsonObject {
            put("version", SharedSessionStore.DOCUMENT_VERSION)
            put("sessions", buildJsonArray {
                add(sessionJson(chinese))
                add(sessionJson(english))
                add(sessionJson(manual))
            })
        }.toString().toByteArray()
        val reloaded = SharedSessionStore.decode(document)
        assertTrue(reloaded.find(chinese.id)?.autoTitle == true)
        assertTrue(reloaded.find(english.id)?.autoTitle == true)
        assertFalse(reloaded.find(manual.id)?.autoTitle ?: true)
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
    fun `creation prefers the selected cell even when another cell comes first`() {
        val store = SharedSessionStore()
        val cells = listOf(SharedCell(cellA, 1024), SharedCell(cellB, 4096))

        val record = store.createInMountedCell(cells, cellB, "selected")

        assertEquals(cellB, record.cellId)
        assertEquals(0, store.sessionCount(cellA))
        assertEquals(listOf(record), store.list(setOf(cellB)))
        assertEquals(SharedContextCodec.tokens(record.context), record.tokens)
        assertEquals(SharedContextCodec.hash(record.context), record.hash)
    }

    @Test
    fun `two mounted cells allow sixteen sessions without manually switching cells`() {
        val store = SharedSessionStore()
        val cells = listOf(SharedCell(cellA, 1024), SharedCell(cellB, 4096))
        val records = (0 until 16).map { store.createInMountedCell(cells, cellA, "session $it") }

        assertEquals(List(8) { cellA } + List(8) { cellB }, records.map { it.cellId })
        assertEquals(8, store.sessionCount(cellA))
        assertEquals(8, store.sessionCount(cellB))
        assertFailsWith<IllegalArgumentException> { store.createInMountedCell(cells, cellA, "seventeenth") }
        assertEquals(records, store.all())
        val reloaded = SharedSessionStore.decode(store.encode())
        assertEquals(records, reloaded.list(setOf(cellA, cellB)))
        assertEquals(store.usedBytes(cellA), reloaded.usedBytes(cellA))
        assertEquals(store.usedBytes(cellB), reloaded.usedBytes(cellB))
    }

    @Test
    fun `mounting another cell expands a previously full network and unmounting excludes it`() {
        val store = SharedSessionStore()
        val first = SharedCell(cellA, 1024)
        val second = SharedCell(cellB, 1024)
        repeat(8) { store.createInMountedCell(listOf(first), cellA, "first $it") }
        assertFailsWith<IllegalArgumentException> { store.createInMountedCell(listOf(first), cellA, "full") }

        val expanded = store.createInMountedCell(listOf(first, second), cellA, "expanded")

        assertEquals(cellB, expanded.cellId)
        assertFailsWith<IllegalArgumentException> { store.createInMountedCell(listOf(first), cellA, "removed") }
        assertEquals(listOf(expanded), store.list(setOf(cellB)))
        assertEquals(8, store.sessionCount(cellA))
    }

    @Test
    fun `creation skips a byte full cell even if it has session slots left`() {
        val store = SharedSessionStore()
        val existing = store.create(cellA, "occupies", context = context(2000))
        val cells = listOf(SharedCell(cellA, existing.charge), SharedCell(cellB, 1024))

        val created = store.createInMountedCell(cells, cellA, "overflow")

        assertEquals(cellB, created.cellId)
        assertEquals(existing, store.find(existing.id))
        assertEquals(existing.charge, store.usedBytes(cellA))
        assertEquals(1, store.sessionCount(cellA))
        assertFailsWith<IllegalArgumentException> {
            store.createInMountedCell(listOf(cells.first(), SharedCell(cellB, created.charge)), cellA, "no bytes")
        }
        assertEquals(listOf(existing, created), store.all())
    }

    @Test
    fun `an unmounted preferred cell cannot redirect creation to a mounted cell`() {
        val store = SharedSessionStore()
        assertFailsWith<IllegalArgumentException> {
            store.createInMountedCell(listOf(SharedCell(cellB, 1024)), cellA, "not mounted")
        }
        assertTrue(store.isEmpty())
    }

    @Test
    fun `duplicate references do not grant extra session slots`() {
        val store = SharedSessionStore()
        val cell = SharedCell(cellA, 1024)
        val cells = listOf(cell, cell)
        repeat(8) { store.createInMountedCell(cells, cellA, "session $it") }

        assertFailsWith<IllegalArgumentException> { store.createInMountedCell(cells, cellA, "duplicate") }
        assertEquals(8, store.sessionCount(cellA))
    }

    @Test
    fun `mounted creation preserves read only storage and rejects invalid titles`() {
        val cells = listOf(SharedCell(cellA, 1024))
        val bytes = byteArrayOf(1, 2, 3)
        val damaged = SharedSessionStore.decode(bytes)
        assertFailsWith<IllegalArgumentException> { damaged.createInMountedCell(cells, cellA, "protected") }
        assertTrue(bytes.contentEquals(damaged.encode()))
        val store = SharedSessionStore()
        assertFailsWith<IllegalArgumentException> { store.createInMountedCell(cells, cellA, " ") }
        assertFailsWith<IllegalArgumentException> {
            store.createInMountedCell(cells, cellA, "t".repeat(SharedSessionLimits.MAX_TITLE_LENGTH + 1))
        }
        assertTrue(store.isEmpty())
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

    @Test
    fun `deleting frees the session slot, the cell's bytes and the mounted view`() {
        val store = SharedSessionStore()
        // Fill the cell's session slots so only a deletion can make room again.
        val doomed = store.create(cellA, "doomed", UUID.randomUUID(), context(5000))
        repeat(SharedSessionLimits.MAX_SESSIONS_PER_CELL - 1) { store.create(cellA, "filler $it") }
        assertEquals(SharedSessionLimits.MAX_SESSIONS_PER_CELL, store.sessionCount(cellA))
        assertFailsWith<IllegalArgumentException> { store.create(cellA, "refused") }
        val occupied = store.usedBytes(cellA)

        val removed = store.delete(doomed.id, doomed.hash)

        assertEquals(doomed, removed)
        assertNull(store.find(doomed.id))
        assertEquals(SharedSessionLimits.MAX_SESSIONS_PER_CELL - 1, store.sessionCount(cellA))
        assertEquals(occupied - doomed.charge, store.usedBytes(cellA))
        assertFalse(store.isMountedIn(doomed.id, setOf(cellA)))
        assertEquals(SharedSessionLimits.MAX_SESSIONS_PER_CELL - 1, store.titles(cellA).size)
        assertFalse(store.titles(cellA).contains("doomed"))

        // Both the freed slot and the freed bytes accept a new conversation again.
        val reuse = store.create(cellA, "reuse", UUID.randomUUID(), context(5000))
        assertEquals(SharedSessionLimits.MAX_SESSIONS_PER_CELL, store.sessionCount(cellA))
        assertEquals(occupied - doomed.charge + reuse.charge, store.usedBytes(cellA))
        val reloaded = SharedSessionStore.decode(store.encode())
        assertEquals(store.all(), reloaded.all())
        assertEquals(store.usedBytes(cellA), reloaded.usedBytes(cellA))
        assertEquals(0L, reloaded.usedBytes(cellB))
    }

    @Test
    fun `a stale or missing session is never deleted`() {
        val store = SharedSessionStore()
        val record = store.create(cellA, "t", UUID.randomUUID(), context(10))
        val occupied = store.usedBytes(cellA)
        val saved = store.replace(record.id, context(20), SharedSessionLimits.CELL_1K_BYTES)

        // The listing the client saw is old: its hash must not be able to remove the newer body.
        assertFailsWith<IllegalArgumentException> { store.delete(record.id, record.hash) }
        assertEquals(saved, store.find(record.id))
        assertEquals(saved.charge, store.usedBytes(cellA))
        assertFailsWith<IllegalArgumentException> { store.delete(UUID.randomUUID(), saved.hash) }
        assertTrue(occupied > 0)

        assertEquals(saved, store.delete(record.id, saved.hash))
        assertNull(store.find(record.id))
        assertEquals(0, store.sessionCount(cellA))
        assertEquals(0L, store.usedBytes(cellA))
    }

    @Test
    fun `protected storage refuses deletion and keeps its bytes`() {
        val bytes = byteArrayOf(1, 2, 3)
        val store = SharedSessionStore.decode(bytes)
        assertFailsWith<IllegalArgumentException> { store.delete(cellA, "") }
        assertTrue(bytes.contentEquals(store.encode()))
    }
}
