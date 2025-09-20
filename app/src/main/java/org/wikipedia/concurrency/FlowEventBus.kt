package org.wikipedia.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wikipedia.util.log.L

object FlowEventBus {

    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    fun post(event: Any) {
        if (!_events.tryEmit(event)) {
            L.e("Unable to emit event")
        }
    }
}

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    fun post(event: AppEvent) {
        if (!_events.tryEmit(event)) {
            _events.tryEmit(event)
        }
    }
}

sealed class AppEvent {
    object ReadingHistoryChanged : AppEvent()
    object ImpactChanged : AppEvent()
    object DonationsChanged : AppEvent()
    object GamesChanged : AppEvent()
}
