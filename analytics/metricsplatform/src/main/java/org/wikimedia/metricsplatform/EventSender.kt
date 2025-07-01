package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.event.EventProcessed
import java.net.URL

fun interface EventSender {
    /**
     * Transmit an event to a destination intake service.
     *
     * @param baseUri base uri of destination intake service
     * @param events events to be sent
     */
    fun sendEvents(baseUri: URL, events: List<EventProcessed>)
}
