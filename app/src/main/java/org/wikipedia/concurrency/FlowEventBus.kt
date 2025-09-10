package org.wikipedia.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wikipedia.activitytab.ActivityTabUpdateEvent
import org.wikipedia.util.log.L

object FlowEventBus {

    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    // Store pending events for ActivityTab
    private val pendingActivityTabEvents = mutableSetOf<ActivityTabUpdateEvent>()

    fun post(event: Any) {
        if (event is ActivityTabUpdateEvent) {
            pendingActivityTabEvents.add(event)
            return
        }

        if (!_events.tryEmit(event)) {
            L.e("Unable to emit event")
        }
    }

    fun consumePendingActivityTabEvents(): Set<ActivityTabUpdateEvent> {
        val events = pendingActivityTabEvents.toSet()
        pendingActivityTabEvents.clear()
        return events
    }

    fun clearPendingActivityTabEvents() {
        pendingActivityTabEvents.clear()
    }
}
