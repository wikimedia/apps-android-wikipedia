package org.wikipedia.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FlowEventBus {

    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    fun post(event: Any) {
        if (!_events.tryEmit(event)) {
            throw IllegalStateException("Unable to emit event")
        }
    }
}
