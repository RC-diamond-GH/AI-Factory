package com.rcdiamond.aifactory.network

import java.util.UUID
import kotlinx.serialization.json.JsonObject
import net.minecraftforge.eventbus.api.Event

/**
 * Posted on the client main thread when an assembled reply to a shared-session request arrives.
 *
 * [payload] follows the `{"ok":true,...}` / `{"ok":false,"error":...}` convention. [requestId] is the id the
 * caller passed to [SharedSessionNetwork.request], so a caller with several requests in flight can match the
 * answer. Nothing is posted for a request whose assembly is still incomplete or was dropped.
 */
class SharedSessionResponseEvent(val requestId: UUID, val payload: JsonObject) : Event()

/**
 * Posted on the client main thread when the server tells the client to open a terminal.
 *
 * [terminal] is the validated locator `{dimension,x,y,z,side}` the client core uses to open the
 * session-selection screen and to name the same terminal part on every later request. The server sends this
 * only after its own block-use validation; the client never trusts a locator it was not sent here.
 */
class SharedTerminalOpenEvent(val terminal: JsonObject) : Event()
