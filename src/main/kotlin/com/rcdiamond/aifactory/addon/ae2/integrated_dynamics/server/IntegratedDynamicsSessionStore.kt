package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.shared.SharedContextCodec
import com.rcdiamond.aifactory.shared.SharedSessionLimits
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** Conversations belong to a terminal identity, never to an ME cell or an ID variable id. */
data class IntegratedDynamicsSessionRecord(
    val id: UUID,
    val terminalId: UUID,
    val title: String,
    val context: JsonObject,
    val hash: String,
    val tokens: Long,
    val autoTitle: Boolean,
)

/** Server-main-thread store. Block entities persist references; the world saves the conversation bodies. */
class IntegratedDynamicsSessionStore {
    private val records = LinkedHashMap<UUID, IntegratedDynamicsSessionRecord>()
    private var preservedDocument: ByteArray? = null

    private fun requireReadable() {
        require(preservedDocument == null) { "Integrated Dynamics session storage is damaged or unsupported; restore a backup" }
    }

    fun find(terminalId: UUID, id: UUID): IntegratedDynamicsSessionRecord? {
        requireReadable()
        return records[id]?.takeIf { it.terminalId == terminalId }
    }

    fun create(terminalId: UUID, title: String, autoTitle: Boolean): IntegratedDynamicsSessionRecord {
        requireReadable()
        require(records.values.count { it.terminalId == terminalId } < MAX_SESSIONS_PER_TERMINAL) {
            "This terminal already holds the maximum of $MAX_SESSIONS_PER_TERMINAL sessions"
        }
        var id = UUID.randomUUID()
        while (records.containsKey(id)) id = UUID.randomUUID()
        val record = canonical(id, terminalId, title, SharedContextCodec.empty(), autoTitle)
        records[id] = record
        return record
    }

    fun requireFresh(terminalId: UUID, id: UUID, hash: String): IntegratedDynamicsSessionRecord {
        val record = find(terminalId, id) ?: throw IllegalArgumentException("No such session on this terminal")
        require(record.hash == hash) { "The session changed on the server; reopen before editing it" }
        return record
    }

    fun replace(
        terminalId: UUID,
        id: UUID,
        expectedHash: String,
        context: JsonObject,
        generatedTitle: String? = null,
    ): IntegratedDynamicsSessionRecord {
        val existing = requireFresh(terminalId, id, expectedHash)
        require(generatedTitle == null || existing.autoTitle) { "This session already has a title" }
        val record = canonical(
            id, terminalId, generatedTitle ?: existing.title, context,
            existing.autoTitle && generatedTitle == null,
        )
        records[id] = record
        return record
    }

    fun delete(terminalId: UUID, id: UUID, expectedHash: String) {
        requireFresh(terminalId, id, expectedHash)
        records.remove(id)
    }

    fun encode(): ByteArray {
        preservedDocument?.let { return it.copyOf() }
        return buildJsonObject {
            put("version", 1)
            put("sessions", buildJsonArray {
                for (record in records.values) add(buildJsonObject {
                    put("id", record.id.toString())
                    put("terminalId", record.terminalId.toString())
                    put("title", record.title)
                    put("autoTitle", record.autoTitle)
                    put("context", record.context)
                })
            })
        }.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val MAX_SESSIONS_PER_TERMINAL = 32
        const val MAX_CONTEXT_BYTES = 4 * 1024 * 1024

        private fun canonical(
            id: UUID, terminalId: UUID, title: String, context: JsonObject, autoTitle: Boolean,
        ): IntegratedDynamicsSessionRecord {
            require(title.isNotBlank() && title.length <= SharedSessionLimits.MAX_TITLE_LENGTH) {
                "A session title must contain 1..${SharedSessionLimits.MAX_TITLE_LENGTH} characters"
            }
            // The shared decoder enforces depth before recursive canonicalisation.
            val normalized = SharedContextCodec.encode(SharedContextCodec.decode(context))
            require(SharedContextCodec.canonicalText(normalized).toByteArray(Charsets.UTF_8).size <= MAX_CONTEXT_BYTES) {
                "An Integrated Dynamics conversation must be at most $MAX_CONTEXT_BYTES bytes"
            }
            return IntegratedDynamicsSessionRecord(
                id, terminalId, title, normalized, SharedContextCodec.hash(normalized),
                SharedContextCodec.tokens(normalized), autoTitle,
            )
        }

        /** Unsupported or damaged data is preserved byte-for-byte and rejects all reads/mutations. */
        fun decode(document: ByteArray): IntegratedDynamicsSessionStore {
            val store = IntegratedDynamicsSessionStore()
            try {
                val root = Json.parseToJsonElement(document.toString(Charsets.UTF_8)) as? JsonObject
                    ?: throw IllegalArgumentException("Invalid session document")
                require((root["version"] as? JsonPrimitive)?.intOrNull == 1)
                val entries = root["sessions"] as? JsonArray ?: throw IllegalArgumentException("Missing sessions")
                val counts = HashMap<UUID, Int>()
                for (element in entries) {
                    val json = element as? JsonObject ?: throw IllegalArgumentException("Invalid session")
                    fun string(key: String): String = (json[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
                        ?: throw IllegalArgumentException("Invalid session field")
                    val id = UUID.fromString(string("id"))
                    val terminalId = UUID.fromString(string("terminalId"))
                    require(!store.records.containsKey(id))
                    val count = (counts[terminalId] ?: 0) + 1
                    require(count <= MAX_SESSIONS_PER_TERMINAL)
                    counts[terminalId] = count
                    val context = json["context"] as? JsonObject ?: throw IllegalArgumentException("Invalid context")
                    val autoTitle = (json["autoTitle"] as? JsonPrimitive)?.booleanOrNull
                        ?: throw IllegalArgumentException("Invalid title state")
                    store.records[id] = canonical(id, terminalId, string("title"), context, autoTitle)
                }
            } catch (_: Exception) {
                store.records.clear()
                store.preservedDocument = document.copyOf()
            }
            return store
        }
    }
}
