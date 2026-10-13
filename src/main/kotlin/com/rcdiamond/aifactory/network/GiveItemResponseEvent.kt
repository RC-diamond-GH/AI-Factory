package com.rcdiamond.aifactory.network

import com.rcdiamond.aifactory.client.llm.GiveItemResult
import net.minecraftforge.eventbus.api.Event

/**
 * Answer of the server to one [GiveItemRequest], fired on the client main thread by [GameActionNetwork].
 *
 * Handlers must correlate [GiveItemResult.requestId] with the request they sent; the event carries no
 * other state, so an unmatched id can be ignored. This event is never fired on a dedicated server.
 */
class GiveItemResponseEvent(val result: GiveItemResult) : Event()
