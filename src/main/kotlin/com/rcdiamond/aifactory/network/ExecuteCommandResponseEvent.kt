package com.rcdiamond.aifactory.network

import com.rcdiamond.aifactory.client.llm.ExecuteCommandResult
import net.minecraftforge.eventbus.api.Event

/** Posted on the client main thread; only the matching pending request consumes this result. */
class ExecuteCommandResponseEvent(val result: ExecuteCommandResult) : Event()
