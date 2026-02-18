package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.event.Event

fun interface EventSender {
    suspend fun sendEvents(destinationEventService: DestinationEventService, events: List<Event>)
}
