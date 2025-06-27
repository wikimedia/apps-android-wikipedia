package org.wikimedia.metrics_platform;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.wikimedia.metrics_platform.event.EventProcessed;

public interface EventSender {

    /**
     * Transmit an event to a destination intake service.
     *
     * @param baseUri base uri of destination intake service
     * @param events events to be sent
     */

    void sendEvents(URL baseUri, Collection<EventProcessed> events) throws IOException;
}
