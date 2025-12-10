package org.wikimedia.testkitchen

import android.net.Uri
import org.wikimedia.testkitchen.event.Event

fun interface EventSender {
    /**
     * Transmit an event to a destination intake service.
     *
     * @param baseUri base uri of destination intake service
     * @param events events to be sent
     */
    fun sendEvents(baseUri: Uri, events: List<Event>)
}
