package com.rcdiamond.aifactory.shared

import com.mojang.logging.LogUtils
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One persisted LLM session: the context that lives in the world save plus the metadata the interface needs.
 *
 * [context] is the canonical ([SharedContextCodec]) form, and [hash]/[tokens] are always derived from it, so
 * a reload can never produce a session whose cached hash disagrees with its body. [charge] is the ME bytes
 * the session occupies in its cell.
 */
data class SharedSessionRecord(
    val id: UUID,
    val cellId: UUID,
    val title: String,
    val context: JsonObject,
    val hash: String,
    val tokens: Long,
    val autoTitle: Boolean = false,
) {
    val charge: Long get() = SharedSessionLimits.chargeFor(tokens)
}

/**
 * The world-save body: every session, keyed by id, with the capacity bookkeeping derived from the contexts.
 *
 * Deliberately a plain object with no Minecraft or AE2 types; [SharedSessionSavedData] is only the thin NBT
 * envelope around [encode]/[decode]. Sessions are keyed by cell id only — a cell keeps its sessions wherever
 * its drive is moved, and a terminal's list is just the filter of the cells its backend reports as installed.
 *
 * All mutation happens on the server main thread. [encode] and [decode] are exact inverses for every store
 * this class builds, which is what makes a save/reload cycle observable in a pure unit test.
 */
class SharedSessionStore {
    private val sessions = LinkedHashMap<UUID, SharedSessionRecord>()
    private class CellUsage(var sessions: Int = 0, var bytes: Long = 0, var titles: List<String>? = null)
    private val usage = HashMap<UUID, CellUsage>()
    private var preservedDocument: ByteArray? = null

    private fun protect(document: ByteArray) {
        if (preservedDocument == null) preservedDocument = document.copyOf()
    }

    private fun requireWritable() {
        if (preservedDocument != null) {
            throw IllegalArgumentException("Shared session storage is damaged or unsupported; restore a backup before editing")
        }
    }

    /** Every session, in insertion (document) order. */
    fun all(): List<SharedSessionRecord> = sessions.values.toList()

    fun find(id: UUID): SharedSessionRecord? = sessions[id]

    /** The sessions whose cell is one of [cellIds]; the order is the document order. */
    fun list(cellIds: Set<UUID>): List<SharedSessionRecord> = sessions.values.filter { it.cellId in cellIds }

    fun sessionCount(cellId: UUID): Int = usage[cellId]?.sessions ?: 0

    /** ME bytes already charged in [cellId]: the sum of the sessions' charges. */
    fun usedBytes(cellId: UUID): Long = usage[cellId]?.bytes ?: 0L

    /** Titles in document order, cached until this cell changes so inventory synchronization does not allocate each tick. */
    fun titles(cellId: UUID): List<String> {
        val cellUsage = usage[cellId] ?: return emptyList()
        cellUsage.titles?.let { return it }
        val titles = ArrayList<String>(cellUsage.sessions)
        for (record in sessions.values) {
            if (record.cellId == cellId) titles.add(record.title)
        }
        cellUsage.titles = titles
        return titles
    }

    fun isEmpty(): Boolean = sessions.isEmpty()

    private fun put(record: SharedSessionRecord) {
        val previous = sessions.put(record.id, record)
        if (previous != null) {
            usage.getValue(previous.cellId).apply {
                sessions--
                bytes -= previous.charge
                if (previous.cellId != record.cellId || previous.title != record.title) titles = null
            }
        }
        usage.getOrPut(record.cellId) { CellUsage() }.apply {
            sessions++
            bytes += record.charge
            if (previous == null || previous.cellId != record.cellId || previous.title != record.title) titles = null
        }
    }

    /**
     * Creates a session in [cellId] with [title] and an initially empty context (or [context]).
     *
     * @throws IllegalArgumentException when the title is blank or too long, or when the cell already holds
     * [SharedSessionLimits.MAX_SESSIONS_PER_CELL] sessions.
     */
    fun create(
        cellId: UUID,
        title: String,
        id: UUID = UUID.randomUUID(),
        context: JsonObject = SharedContextCodec.empty(),
        autoTitle: Boolean = false,
    ): SharedSessionRecord {
        requireWritable()
        requireTitle(title)
        if (sessionCount(cellId) >= SharedSessionLimits.MAX_SESSIONS_PER_CELL) {
            throw IllegalArgumentException(
                "This cell already holds the maximum of ${SharedSessionLimits.MAX_SESSIONS_PER_CELL} sessions",
            )
        }
        if (sessions.containsKey(id)) throw IllegalArgumentException("Session $id already exists")
        val record = canonicalRecord(id, cellId, title, context, autoTitle)
        put(record)
        return record
    }

    /**
     * Creates an empty session on the preferred mounted cell, or another mounted cell with room.
     * Session slots and byte budgets remain per cell; existing sessions are never moved.
     */
    fun createInMountedCell(
        cells: List<SharedCell>,
        preferredCellId: UUID,
        title: String,
        autoTitle: Boolean = false,
    ): SharedSessionRecord {
        requireWritable()
        requireTitle(title)
        val preferred = cells.firstOrNull { it.id == preferredCellId }
            ?: throw IllegalArgumentException("That cell is not installed in this terminal")
        val context = SharedContextCodec.empty()
        val tokens = SharedContextCodec.tokens(context)
        val charge = SharedSessionLimits.chargeFor(tokens)
        fun hasRoom(cell: SharedCell): Boolean =
            sessionCount(cell.id) < SharedSessionLimits.MAX_SESSIONS_PER_CELL &&
                usedBytes(cell.id) + charge <= cell.bytes

        val cell = if (hasRoom(preferred)) preferred else cells.firstOrNull { hasRoom(it) }
            ?: throw IllegalArgumentException("No installed intelligent cell has room for another session")
        val id = UUID.randomUUID()
        if (sessions.containsKey(id)) throw IllegalArgumentException("Session $id already exists")
        val record = SharedSessionRecord(id, cell.id, title, context, SharedContextCodec.hash(context), tokens, autoTitle)
        put(record)
        return record
    }

    private fun requireTitle(title: String) {
        if (title.isBlank()) throw IllegalArgumentException("A session title must not be blank")
        if (title.length > SharedSessionLimits.MAX_TITLE_LENGTH) {
            throw IllegalArgumentException("A session title must be at most ${SharedSessionLimits.MAX_TITLE_LENGTH} characters")
        }
    }

    /**
     * Replaces the body and, only for an automatically named session, its first generated title atomically.
     *
     * @throws IllegalArgumentException when the session does not exist.
     */
    fun replace(id: UUID, context: JsonObject, cellBytes: Long, generatedTitle: String? = null): SharedSessionRecord {
        requireWritable()
        val existing = sessions[id] ?: throw IllegalArgumentException("No such session")
        if (generatedTitle != null) {
            requireTitle(generatedTitle)
            require(existing.autoTitle) { "This session already has a title" }
        }
        val record = canonicalRecord(
            existing.id, existing.cellId, generatedTitle ?: existing.title, context,
            existing.autoTitle && generatedTitle == null,
        )
        requireRoom(existing.cellId, cellBytes, id, record.tokens)
        put(record)
        return record
    }

    /**
     * Removes [id] from the store, freeing its session slot and its ME bytes in its cell.
     *
     * The optimistic guard runs first: [expectedHash] is the hash the caller last saw, so a stale listing
     * can never destroy a conversation somebody else has since replaced.
     *
     * @throws IllegalArgumentException when the session does not exist, when its stored hash no longer
     * equals [expectedHash], or when the document is protected read-only.
     */
    fun delete(id: UUID, expectedHash: String): SharedSessionRecord {
        requireWritable()
        val record = sessions[id] ?: throw IllegalArgumentException("No such session")
        if (record.hash != expectedHash) {
            throw IllegalArgumentException("The session changed on the server; refresh before deleting it")
        }
        sessions.remove(id)
        usage.getValue(record.cellId).apply {
            sessions--
            bytes -= record.charge
            titles = null
        }
        return record
    }

    /** Whether [id] exists and its cell is one of [cellIds]. */
    fun isMountedIn(id: UUID, cellIds: Set<UUID>): Boolean {
        val record = sessions[id] ?: return false
        return record.cellId in cellIds
    }

    /**
     * The optimistic-update guard: returns the session [id] only when its stored hash still equals
     * [expectedHash].
     *
     * @throws IllegalArgumentException when the session is gone or another writer changed it since the
     * client last saw it. The caller must then reopen the session instead of saving over the newer body.
     */
    fun requireFresh(id: UUID, expectedHash: String): SharedSessionRecord {
        val record = sessions[id] ?: throw IllegalArgumentException("No such session")
        if (record.hash != expectedHash) {
            throw IllegalArgumentException("The session changed on the server; reopen it before saving")
        }
        return record
    }

    /**
     * The capacity guard: checks that [tokens] still fit in [cellId] whose capacity is [cellBytes], counting
     * the session [replacing] (if any) as already removed.
     *
     * @throws IllegalArgumentException when the new charge would exceed the cell's shared ME byte budget.
     */
    fun requireRoom(cellId: UUID, cellBytes: Long, replacing: UUID?, tokens: Long) {
        val charge = SharedSessionLimits.chargeFor(tokens)
        val current = usedBytes(cellId) - (replacing?.let { sessions[it]?.charge } ?: 0L)
        if (current + charge > cellBytes) {
            throw IllegalArgumentException("This cell does not have room for the context")
        }
    }

    fun encode(): ByteArray {
        preservedDocument?.let { return it.copyOf() }
        val document = buildJsonObject {
            put("version", DOCUMENT_VERSION)
            put(
                "sessions",
                buildJsonArray {
                    sessions.values.forEach { record ->
                        add(
                            buildJsonObject {
                                put("id", record.id.toString())
                                put("cellId", record.cellId.toString())
                                put("title", record.title)
                                put("autoTitle", record.autoTitle)
                                put("context", record.context)
                            },
                        )
                    }
                },
            )
        }
        return document.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        /** Version of the encoded document; a mismatch is rejected instead of being guessed at. */
        const val DOCUMENT_VERSION: Int = 1

        private val LOGGER = LogUtils.getLogger()

        /**
         * Reads a document produced by [encode]. Damaged or unsupported documents keep their original
         * bytes and become read-only, preventing a later save from silently replacing data with an empty
         * store. Readable entries remain available for inspection.
         */
        fun decode(bytes: ByteArray): SharedSessionStore {
            val store = SharedSessionStore()
            val document = try {
                Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)) as? JsonObject
            } catch (error: Exception) {
                LOGGER.warn("Preserving an unreadable shared-session document in read-only mode")
                store.protect(bytes)
                return store
            }
            if (document == null) {
                LOGGER.warn("Preserving a non-object shared-session document in read-only mode")
                store.protect(bytes)
                return store
            }
            val version = (document["version"] as? JsonPrimitive)?.content?.toIntOrNull()
            if (version != DOCUMENT_VERSION) {
                LOGGER.warn("Preserving unsupported shared-session version {} in read-only mode", version)
                store.protect(bytes)
                return store
            }
            val entries = document["sessions"] as? JsonArray
            if (entries == null) {
                store.protect(bytes)
                return store
            }
            for (element in entries) {
                val record = try {
                    decodeRecord(element as? JsonObject ?: throw IllegalArgumentException("a session is not an object"))
                } catch (error: Exception) {
                    LOGGER.warn("An unreadable shared session makes this document read-only")
                    store.protect(bytes)
                    continue
                }
                if (store.sessionCount(record.cellId) >= SharedSessionLimits.MAX_SESSIONS_PER_CELL) {
                    LOGGER.warn("Cell {} exceeds its session limit; preserving the document read-only", record.cellId)
                    store.protect(bytes)
                    continue
                }
                store.put(record)
            }
            return store
        }

        private fun decodeRecord(element: JsonObject): SharedSessionRecord {
            val id = UUID.fromString(element.requireString("id"))
            val cellId = UUID.fromString(element.requireString("cellId"))
            val title = element.requireString("title")
            val context = element["context"] as? JsonObject
                ?: throw IllegalArgumentException("a session has no context object")
            // Older clients did not distinguish an omitted title from these translated placeholders.
            val autoTitle = if ("autoTitle" in element) element.requireBoolean("autoTitle")
                else title == "新会话" || title == "New conversation"
            return canonicalRecord(id, cellId, title, context, autoTitle)
        }
    }
}

/**
 * Builds a record whose body is canonical, deriving the hash and token count from that body so a reload can
 * never produce metadata that disagrees with the stored context.
 */
private fun canonicalRecord(
    id: UUID,
    cellId: UUID,
    title: String,
    context: JsonObject,
    autoTitle: Boolean = false,
): SharedSessionRecord {
    val canonical = SharedContextCodec.encode(SharedContextCodec.decode(context))
    return SharedSessionRecord(
        id = id,
        cellId = cellId,
        title = title,
        context = canonical,
        hash = SharedContextCodec.hash(canonical),
        tokens = SharedContextCodec.tokens(canonical),
        autoTitle = autoTitle,
    )
}
