package com.rcdiamond.aifactory.network

import java.util.UUID

/**
 * One chunk of a client-to-server shared-session request.
 *
 * A request body (an `open` with a whole context, say) can be far larger than one Minecraft packet, so it is
 * split into chunks of at most [SharedSessionNetwork.MAX_CHUNK_BYTES] and reassembled on the server. Chunks
 * of one request share [requestId]; [chunkIndex] counts from zero and must arrive in order, and [chunkCount]
 * is the total. The direction is fixed: this message only travels to the server.
 */
class SharedSessionRequestMessage(
    val requestId: UUID,
    val chunkIndex: Int,
    val chunkCount: Int,
    val data: ByteArray,
)

/**
 * One chunk of a server-to-client shared-session reply. Same framing as [SharedSessionRequestMessage]; the
 * direction is fixed: this message only travels to the client.
 */
class SharedSessionResponseMessage(
    val requestId: UUID,
    val chunkIndex: Int,
    val chunkCount: Int,
    val data: ByteArray,
)

/**
 * Asks the client to open the terminal at [locator]. The locator is a bounded `{dimension,x,y,z}` JSON text;
 * it is small enough to travel whole, so it is not chunked.
 */
class SharedTerminalOpenMessage(val locator: String)
