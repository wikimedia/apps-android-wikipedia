package org.wikimedia.metricsplatform

import android.net.Uri
import org.wikimedia.metricsplatform.event.EventProcessed

fun interface EventSender {
    /**
     * Transmit an event to a destination intake service.
     *
     * @param baseUri base uri of destination intake service
     * @param events events to be sent
     */
    fun sendEvents(baseUri: Uri, events: List<EventProcessed>)
}
