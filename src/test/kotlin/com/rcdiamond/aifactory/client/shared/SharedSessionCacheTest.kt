package com.rcdiamond.aifactory.client.shared

import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The cache is the client's copy of a conversation the server owns; deleting a session must therefore purge
 * the matching entry, and purging one that is already gone must stay harmless. The entry body is opaque to
 * the cache, so a tiny placeholder context is enough to pin the file lifecycle.
 */
class SharedSessionCacheTest {

    private fun entry() = CachedSharedSession("hash", 42L, buildJsonObject { put("marker", "cached") })

    @Test
    fun `removing a deleted session drops its cached conversation`() = runBlocking {
        val root = Files.createTempDirectory("shared-session-cache")
        val cache = SharedSessionCache(root)
        val world = "server_test"
        val session = UUID.randomUUID()
        val untouched = UUID.randomUUID()
        cache.write(world, session, entry())
        cache.write(world, untouched, entry())
        assertEquals(entry(), cache.read(world, session))

        cache.remove(world, session)

        assertNull(cache.read(world, session))
        // Only the deleted session is dropped; its neighbour in the same world survives.
        assertNotNull(cache.read(world, untouched))
        // Purging again, or a session that was never cached, is not an error.
        cache.remove(world, session)
        cache.remove(world, UUID.randomUUID())
    }
}
