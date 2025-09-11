package org.wikipedia.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wikipedia.activitytab.ActivityTabUpdateEvent
import org.wikipedia.util.log.L

object FlowEventBus {

    private val _events = MutableSharedFlow<Any>(replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    fun post(event: Any) {
        if (!_events.tryEmit(event)) {
            L.e("Unable to emit event")
        }
    }
}

object ActivityTabEventBus {
    private val _events = MutableSharedFlow<ActivityTabUpdateEvent>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun post(event: ActivityTabUpdateEvent) {
        println("orange ActivityTabEventBus post event $event")
        if (!_events.tryEmit(event)) {
            _events.tryEmit(event)
        }
    }
}