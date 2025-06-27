package org.wikimedia.metrics_platform.event;

import static org.wikimedia.metrics_platform.context.DataFixtures.getTestClientData;
import static org.wikimedia.metrics_platform.event.EventProcessed.fromEvent;

import java.util.Arrays;
import java.util.List;

import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;

public final class EventFixtures {

    private EventFixtures() {
        // Utility class, should never be instantiated
    }
    public static Event minimalEvent() {
        return new Event("test_schema", "test_stream", "test_event");
    }

    public static EventProcessed minimalEventProcessed() {
        EventProcessed eventProcessed = fromEvent(minimalEvent());
        eventProcessed.setClientData(getTestClientData());
        return eventProcessed;
    }

    public static EventProcessed getEvent() {
        Event event = new Event("test/event", "test.event", "testEvent");
        ClientData clientData = new ClientData();
        clientData.setPageData(PageData.builder().id(1).namespaceName("Talk").build());

        event.setClientData(clientData);
        EventProcessed eventProcessed = fromEvent(event);
        eventProcessed.setPerformerData(
                PerformerData.builder()
                        .groups(Arrays.asList("user", "autoconfirmed", "steward"))
                        .isLoggedIn(true)
                        .build()
        );
        return eventProcessed;
    }

    public static EventProcessed getEvent(
            Integer id,
            String namespaceName,
            List<String> groups,
            boolean isLoggedIn,
            String editCount
    ) {
        Event event = new Event("test/event", "test.event", "testEvent");
        ClientData clientData = new ClientData();
        clientData.setPageData(PageData.builder().id(id).namespaceName(namespaceName).build());

        event.setClientData(clientData);
        EventProcessed eventProcessed = fromEvent(event);
        eventProcessed.setPerformerData(
                PerformerData.builder()
                        .groups(groups)
                        .isLoggedIn(isLoggedIn)
                        .build()
        );
        return eventProcessed;
    }
}
