package org.wikimedia.metrics_platform;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.wikimedia.metrics_platform.event.EventProcessed;

class TestEventSender implements EventSender {

    private final boolean shouldFail;

    TestEventSender() {
        this(false);
    }

    TestEventSender(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public void sendEvents(URL baseUri, Collection<EventProcessed> events) throws IOException {
        if (shouldFail) {
            throw new IOException();
        }
    }
}
