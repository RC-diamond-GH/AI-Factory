package com.rcdiamond.aifactory.shared

import java.util.UUID
import kotlinx.serialization.json.JsonObject

/**
 * Server-side exclusive claim one player holds on one session of one terminal.
 *
 * The lease is the authority the client cannot forge: it binds the *real* sender resolved from the
 * connection, the session id and the terminal locator, and every request that names a session must match
 * its holder. It also carries the two interface flags the release rule is built on. The initial acquired
 * state is `screenOpen = true` because the client only opens a session to show its screen, and
 * `running = false` because no agent turn has been started yet.
 *
 * Leases are runtime-only: they are not persisted, so a server restart (and every player logout) drops them
 * and frees the sessions. Losing a lease this way is fail-closed — the client must `save` before it can act
 * again, and an in-flight background turn is not resumed for a lease the server no longer knows about.
 */
class SharedSessionLease internal constructor(
    /** The real sender's UUID; never taken from a request payload. */
    val player: UUID,
    val sessionId: UUID,
    var cellId: UUID,
    /** The terminal locator the session was opened through, revalidated on every later request. */
    var terminal: JsonObject,
) {
    var screenOpen: Boolean = true
    var running: Boolean = false

    /**
     * The release predicate: a session is freed exactly when the screen is closed *and* no turn is running.
     * An open screen or a running turn always keeps the lease, including a background turn whose screen was
     * closed — closing the screen must never cancel shared work.
     */
    fun shouldRelease(): Boolean = !screenOpen && !running

    /** Applies the state the client reports; the caller then checks [shouldRelease]. */
    fun update(screenOpen: Boolean, running: Boolean) {
        this.screenOpen = screenOpen
        this.running = running
    }
}

/**
 * Registry of the live leases: one owned session per player, one owner per session.
 *
 * A player may reopen the session they already own (for example after a reconnect or a screen refresh);
 * opening a different session, or a session owned by somebody else, is a conflict. All access is on the
 * server main thread.
 */
class SharedSessionLeases {
    private val byPlayer = HashMap<UUID, SharedSessionLease>()
    private val bySession = HashMap<UUID, SharedSessionLease>()

    fun leaseFor(player: UUID): SharedSessionLease? = byPlayer[player]

    /** The UUID of the player currently holding [sessionId], or `null`. */
    fun holderOf(sessionId: UUID): UUID? = bySession[sessionId]?.player

    fun size(): Int = byPlayer.size

    /**
     * Atomically takes (or refreshes) the lease on [sessionId] for [player].
     *
     * @throws IllegalArgumentException when the player already owns a *different* session, or when
     * [sessionId] is held by another player. The message is safe to show the requesting player.
     */
    fun acquire(player: UUID, sessionId: UUID, cellId: UUID, terminal: JsonObject): SharedSessionLease {
        val mine = byPlayer[player]
        if (mine != null) {
            if (mine.sessionId != sessionId) {
                throw IllegalArgumentException("You already have another session open; close it before opening this one")
            }
            mine.cellId = cellId
            mine.terminal = terminal
            mine.screenOpen = true
            return mine
        }
        val other = bySession[sessionId]
        if (other != null && other.player != player) {
            throw IllegalArgumentException("That session is already in use by another player")
        }
        val lease = SharedSessionLease(player, sessionId, cellId, terminal)
        byPlayer[player] = lease
        bySession[sessionId] = lease
        return lease
    }

    /**
     * Authorizes deleting [sessionId] for [player] and returns the caller's *own* lease on it when there is
     * one, so the caller can release it only after the store mutation succeeded.
     *
     * A session held by another player is never deletable, and neither is a session whose turn is still
     * running — including the caller's own, whose unsaved conversation must not be thrown away underneath it.
     * An idle lease of the caller's own does not block deletion: the conversation is about to disappear, so
     * nothing needs to be flushed first.
     *
     * @throws IllegalArgumentException with a message safe to show the requesting player.
     */
    fun authorizeDeletion(player: UUID, sessionId: UUID): SharedSessionLease? {
        val holder = bySession[sessionId]
        if (holder != null && holder.player != player) {
            throw IllegalArgumentException("That session is already in use by another player")
        }
        val mine = byPlayer[player]?.takeIf { it.sessionId == sessionId }
        if (mine != null && mine.running) {
            throw IllegalArgumentException("That session has a running turn; stop it before deleting it")
        }
        return mine
    }

    /** Frees the lease held by [player], if any. Returns the released lease. */
    fun release(player: UUID): SharedSessionLease? {
        val lease = byPlayer.remove(player) ?: return null
        bySession.remove(lease.sessionId)
        return lease
    }

    /** Drops every lease; used when the server stops, where the state is not worth persisting. */
    fun clear() {
        byPlayer.clear()
        bySession.clear()
    }
}
