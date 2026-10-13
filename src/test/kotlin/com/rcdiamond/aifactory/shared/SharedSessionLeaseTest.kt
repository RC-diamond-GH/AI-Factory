package com.rcdiamond.aifactory.shared

import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SharedSessionLeaseTest {

    private val alice: UUID = UUID.randomUUID()
    private val bob: UUID = UUID.randomUUID()
    private val session: UUID = UUID.randomUUID()
    private val other: UUID = UUID.randomUUID()
    private val cell: UUID = UUID.randomUUID()

    private fun terminal(x: Int = 1): JsonObject = buildJsonObject {
        put("dimension", "minecraft:overworld")
        put("x", x)
        put("y", 64)
        put("z", 0)
    }

    @Test
    fun `a fresh lease starts with the screen open and nothing running`() {
        val lease = SharedSessionLeases().acquire(alice, session, cell, terminal())
        assertTrue(lease.screenOpen)
        assertFalse(lease.running)
        assertFalse(lease.shouldRelease())
    }

    @Test
    fun `closing the screen while a turn runs keeps the lease`() {
        val lease = SharedSessionLeases().acquire(alice, session, cell, terminal())
        lease.update(screenOpen = false, running = true)
        assertFalse(lease.shouldRelease())
    }

    @Test
    fun `a finished turn with the screen still open keeps the lease`() {
        val lease = SharedSessionLeases().acquire(alice, session, cell, terminal())
        lease.update(screenOpen = true, running = false)
        assertFalse(lease.shouldRelease())
    }

    @Test
    fun `the lease is released only when the screen is closed and the turn stopped`() {
        val leases = SharedSessionLeases()
        val lease = leases.acquire(alice, session, cell, terminal())

        lease.update(screenOpen = false, running = true)
        assertFalse(lease.shouldRelease())
        lease.update(screenOpen = false, running = false)
        assertTrue(lease.shouldRelease())

        leases.release(alice)
        assertNull(leases.leaseFor(alice))
        assertNull(leases.holderOf(session))
        assertEquals(0, leases.size())
    }

    @Test
    fun `a player may reopen the session they already own`() {
        val leases = SharedSessionLeases()
        val first = leases.acquire(alice, session, cell, terminal(1))
        first.update(screenOpen = false, running = true)

        val again = leases.acquire(alice, session, cell, terminal(2))
        assertSame(first, again)
        assertEquals(1, leases.size())
        // Reopening re-shows the screen but must not cancel the shared turn that is still running.
        assertTrue(again.screenOpen)
        assertTrue(again.running)
        assertEquals(terminal(2), again.terminal)
    }

    @Test
    fun `a player may not own a second session`() {
        val leases = SharedSessionLeases()
        leases.acquire(alice, session, cell, terminal())
        assertFailsWith<IllegalArgumentException> { leases.acquire(alice, other, cell, terminal()) }
        assertEquals(1, leases.size())
    }

    @Test
    fun `a session already in use by another player is a conflict`() {
        val leases = SharedSessionLeases()
        leases.acquire(alice, session, cell, terminal())
        assertFailsWith<IllegalArgumentException> { leases.acquire(bob, session, cell, terminal()) }
        assertEquals(alice, leases.holderOf(session))
        assertEquals(1, leases.size())
    }

    @Test
    fun `releasing frees the session for another player`() {
        val leases = SharedSessionLeases()
        leases.acquire(alice, session, cell, terminal())
        leases.release(alice)
        assertNull(leases.holderOf(session))

        val bobLease = leases.acquire(bob, session, cell, terminal())
        assertEquals(bob, leases.holderOf(session))
        assertEquals(session, bobLease.sessionId)
    }

    @Test
    fun `clear drops every lease`() {
        val leases = SharedSessionLeases()
        leases.acquire(alice, session, cell, terminal())
        leases.acquire(bob, other, cell, terminal())
        assertEquals(2, leases.size())

        leases.clear()
        assertEquals(0, leases.size())
        assertNull(leases.leaseFor(alice))
        assertNull(leases.holderOf(other))
    }
}
