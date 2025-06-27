package org.wikimedia.metrics_platform;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metrics_platform.ConsistencyITClientData.createConsistencyTestClientData;
import static org.wikimedia.metrics_platform.config.StreamConfigFetcher.ANALYTICS_API_ENDPOINT;
import static org.wikimedia.metrics_platform.MetricsClient.METRICS_PLATFORM_SCHEMA_BASE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.config.SourceConfig;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.config.StreamConfigFetcher;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.DataFixtures;
import org.wikimedia.metrics_platform.event.EventProcessed;
import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;

class ConsistencyIT {
    private JsonObject expectedEvent;

    @Test void testConsistency() throws IOException {
        Path pathStreamConfigs = Paths.get("../tests/consistency/stream_configs_apps.json");

        try (BufferedReader reader = Files.newBufferedReader(pathStreamConfigs)) {

            // Init shared test config + variables for creating new metrics client + event processor.
            Map<String, StreamConfig> testStreamConfigs = getTestStreamConfigs(reader);
            SourceConfig sourceConfig = new SourceConfig(testStreamConfigs);
            AtomicReference<SourceConfig> sourceConfigRef = new AtomicReference<>();
            sourceConfigRef.set(sourceConfig);
            BlockingQueue<EventProcessed> eventQueue = new LinkedBlockingQueue<>(10);
            ClientData consistencyTestClientData = createConsistencyTestClientData();
            SamplingController samplingController = new SamplingController(consistencyTestClientData, new SessionController());

            EventProcessor consistencyTestEventProcessor = getTestEventProcessor(
                    sourceConfigRef,
                    samplingController,
                    eventQueue
            );

            MetricsClient consistencyTestMetricsClient = getTestMetricsClient(
                    consistencyTestClientData,
                    sourceConfigRef,
                    eventQueue
            );

            consistencyTestMetricsClient.submitMetricsEvent(
                    "test.consistency",
                    METRICS_PLATFORM_SCHEMA_BASE,
                    "test_consistency_event",
                    DataFixtures.getTestClientData(getExpectedEventJson().toString()),
                    singletonMap("test", "consistency")
            );

            EventProcessed queuedEvent = eventQueue.peek();
            consistencyTestEventProcessor.eventPassesCurationRules(queuedEvent, testStreamConfigs);

            // Adjust the queuedEvent and compare it against the expected event.
            if (this.expectedEvent != null) {
                Gson gson = GsonHelper.getGson();
                String queuedEventJsonStringRaw = gson.toJson(queuedEvent);
                JsonObject queuedEventJsonObject = JsonParser.parseString(queuedEventJsonStringRaw).getAsJsonObject();
                // Remove the timestamp properties from the queued event to match the expected event json.
                removeExtraProperties(queuedEventJsonObject);

                assertThat(queuedEventJsonObject)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrder()
                        .isEqualTo(this.expectedEvent);
            }
        }
    }

    private static MetricsClient getTestMetricsClient(
            ClientData consistencyTestClientData,
            AtomicReference<SourceConfig> sourceConfigRef,
            BlockingQueue<EventProcessed> eventQueue
    ) {
        return MetricsClient.builder(consistencyTestClientData)
                .sourceConfigRef(sourceConfigRef)
                .eventQueue(eventQueue)
                .build();
    }

    private static EventProcessor getTestEventProcessor(
            AtomicReference<SourceConfig> sourceConfigRef,
            SamplingController samplingController,
            BlockingQueue<EventProcessed> eventQueue
    ) {
        ContextController contextController = new ContextController();
        CurationController curationController = new CurationController();
        EventSender eventSender = new TestEventSender();
        return new EventProcessor(
                contextController,
                curationController,
                sourceConfigRef,
                samplingController,
                eventSender,
                eventQueue,
                true
        );
    }

    private static Map<String, StreamConfig> getTestStreamConfigs(Reader reader) throws MalformedURLException {
        StreamConfigFetcher streamConfigFetcher = new StreamConfigFetcher(new URL(ANALYTICS_API_ENDPOINT), new OkHttpClient(), GsonHelper.getGson());
        return streamConfigFetcher.parseConfig(reader);
    }

    private static void removeExtraProperties(JsonObject eventJsonObject) {
        eventJsonObject.remove("dt");
        eventJsonObject.remove("sample");
        eventJsonObject.getAsJsonObject("performer").remove("registration_dt");
    }

    private JsonObject getExpectedEventJson() throws IOException {
        if (this.expectedEvent == null) {
            Path pathExpectedEvent = Paths.get("../tests/consistency/expected_event_apps.json");
            try (BufferedReader expectedEventReader = Files.newBufferedReader(pathExpectedEvent)) {
                this.expectedEvent = JsonParser.parseReader(expectedEventReader).getAsJsonObject();
            }
        }
        return this.expectedEvent;
    }
}
