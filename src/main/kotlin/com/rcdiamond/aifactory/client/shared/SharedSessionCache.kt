package com.rcdiamond.aifactory.client.shared

import com.mojang.logging.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import net.minecraftforge.fml.loading.FMLPaths
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Last server-acknowledged conversation of one shared session, kept on this client's disk.
 *
 * [hash] is the hash the server reported for [context], so a reopen can send it back and only fetch the
 * conversation when the server reports a different one. The context never contains credentials.
 */
data class CachedSharedSession(val hash: String, val tokens: Long, val context: JsonObject)

/**
 * Per-world, per-session cache of the last synchronized shared conversation.
 *
 * Files live under `config/ai-factory/shared-sessions/<world>/<session uuid>.json`, so a session is keyed by
 * both its world and its UUID and two worlds can never collide. All file IO runs on the IO dispatcher and
 * never throws: a cache miss, a corrupt entry or an unwritable directory only means the next open fetches
 * the conversation from the server again.
 *
 * An entry is written through a temporary file and then moved into place, so an interrupted write cannot
 * leave a half-written context behind. When a world accumulates more than [MAX_SESSIONS_PER_WORLD] entries
 * the least recently written ones are dropped; dropping an entry only costs one server fetch on reopen.
 */
class SharedSessionCache(private val root: Path = defaultRoot()) {
    suspend fun read(worldKey: String, sessionId: UUID): CachedSharedSession? = withContext(Dispatchers.IO) {
        val file = fileFor(worldKey, sessionId)
        if (!Files.isRegularFile(file)) return@withContext null
        val text = try {
            Files.readString(file)
        } catch (ignored: IOException) {
            LOGGER.warn("Could not read the cached shared session {}", sessionId)
            return@withContext null
        }
        val parsed = try {
            Json.parseToJsonElement(text) as? JsonObject
        } catch (ignored: SerializationException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null
        }
        val entry = parsed?.let(::readEntry)
        if (entry == null) {
            LOGGER.warn("Ignoring the malformed cached shared session {}", sessionId)
            return@withContext null
        }
        entry
    }

    suspend fun write(worldKey: String, sessionId: UUID, entry: CachedSharedSession): Unit =
        withContext(Dispatchers.IO) {
            val directory = directoryFor(worldKey)
            val target = fileFor(worldKey, sessionId)
            val payload = buildJsonObject {
                put("version", FORMAT_VERSION)
                put("hash", entry.hash)
                put("tokens", entry.tokens)
                put("context", entry.context)
            }.toString()
            val temporary = target.resolveSibling("$sessionId.json.tmp")
            try {
                Files.createDirectories(directory)
                Files.writeString(
                    temporary, payload,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                )
                moveIntoPlace(temporary, target)
            } catch (ignored: IOException) {
                LOGGER.warn("Could not cache the shared session {}", sessionId)
                runCatching { Files.deleteIfExists(temporary) }
                return@withContext
            }
            prune(directory)
        }

    /**
     * Drops the cached conversation of [sessionId] in [worldKey], used once the server confirms the session
     * itself is gone. A missing entry is not an error: the two stores are independent.
     */
    suspend fun remove(worldKey: String, sessionId: UUID): Unit = withContext(Dispatchers.IO) {
        val target = fileFor(worldKey, sessionId)
        try {
            Files.deleteIfExists(target)
        } catch (ignored: IOException) {
            LOGGER.warn("Could not drop the cached shared session {}", sessionId)
        }
        runCatching { Files.deleteIfExists(target.resolveSibling("$sessionId.json.tmp")) }
    }

    private fun readEntry(json: JsonObject): CachedSharedSession? {
        val hash = (json["hash"] as? JsonPrimitive)?.contentOrNull ?: return null
        val tokens = (json["tokens"] as? JsonPrimitive)?.longOrNull ?: 0L
        val context = json["context"] as? JsonObject ?: return null
        return CachedSharedSession(hash, tokens, context)
    }

    private fun moveIntoPlace(temporary: Path, target: Path) {
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (ignored: AtomicMoveNotSupportedException) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Drops the least recently written entries once a world holds more than [MAX_SESSIONS_PER_WORLD]. */
    private fun prune(directory: Path) {
        val files = try {
            Files.newDirectoryStream(directory, "*$ENTRY_SUFFIX").use { it.toList() }
        } catch (ignored: IOException) {
            return
        }
        if (files.size <= MAX_SESSIONS_PER_WORLD) return
        val doomed = files.sortedBy { lastModified(it) }.take(files.size - MAX_SESSIONS_PER_WORLD)
        for (file in doomed) {
            runCatching { Files.deleteIfExists(file) }
        }
    }

    private fun lastModified(file: Path): Long = try {
        Files.getLastModifiedTime(file).toMillis()
    } catch (ignored: IOException) {
        Long.MAX_VALUE
    }

    private fun directoryFor(worldKey: String): Path = root.resolve(sanitize(worldKey))

    private fun fileFor(worldKey: String, sessionId: UUID): Path =
        directoryFor(worldKey).resolve("$sessionId$ENTRY_SUFFIX")

    companion object {
        private val LOGGER = LogUtils.getLogger()
        private const val FORMAT_VERSION = 1
        private const val MAX_SESSIONS_PER_WORLD = 32
        private const val ENTRY_SUFFIX = ".json"
        private const val MAX_KEY_LENGTH = 120

        private fun defaultRoot(): Path =
            FMLPaths.CONFIGDIR.get().resolve("ai-factory").resolve("shared-sessions")

        /**
         * Turns a world identifier into one safe, unique path segment. Characters outside the portable set
         * become underscores and the segment is length-bounded; a stable hash of the original key is always
         * appended so two worlds whose names differ only in replaced characters cannot share a directory.
         */
        fun sanitize(worldKey: String): String {
            val cleaned = buildString(worldKey.length) {
                for (character in worldKey) {
                    val portable = character.isLetterOrDigit() &&
                        character.code < 128 || character == '.' || character == '-' || character == '_'
                    append(if (portable) character else '_')
                }
            }.trim('.')
            val base = if (cleaned.isEmpty()) "world" else cleaned.take(MAX_KEY_LENGTH - 9)
            return base + "_" + Integer.toHexString(worldKey.hashCode())
        }
    }
}
