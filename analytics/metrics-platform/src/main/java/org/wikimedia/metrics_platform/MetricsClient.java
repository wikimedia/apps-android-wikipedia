package org.wikimedia.metrics_platform;

import static java.lang.Math.max;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toList;
import static org.wikimedia.metrics_platform.config.StreamConfigFetcher.ANALYTICS_API_ENDPOINT;
import static org.wikimedia.metrics_platform.event.EventProcessed.fromEvent;

import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;

import org.wikimedia.metrics_platform.config.ConfigFetcherRunnable;
import org.wikimedia.metrics_platform.config.SourceConfig;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.config.StreamConfigFetcher;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.InteractionData;
import org.wikimedia.metrics_platform.context.PerformerData;
import org.wikimedia.metrics_platform.event.Event;
import org.wikimedia.metrics_platform.event.EventProcessed;
import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import okhttp3.OkHttpClient;

@Log
public final class MetricsClient {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
            .withZone(ZoneId.of("UTC"));

    private final ScheduledExecutorService executorService;
    public static final String METRICS_PLATFORM_LIBRARY_VERSION = "2.8";
    public static final String METRICS_PLATFORM_BASE_VERSION = "1.2.2";
    public static final String METRICS_PLATFORM_SCHEMA_BASE = "/analytics/product_metrics/app/base/" + METRICS_PLATFORM_BASE_VERSION;
    private final AtomicReference<SourceConfig> sourceConfig;

    /**
     * Handles logging session management. A new session begins (and a new session ID is created)
     * if the app has been inactive for 15 minutes or more.
     */
    private final SessionController sessionController;

    /**
     * Evaluates whether events for a given stream are in-sample based on the stream configuration.
     */
    private final SamplingController samplingController;

    private final BlockingQueue<EventProcessed> eventQueue;
    private final EventProcessor eventProcessor;

    /**
     * MetricsClient constructor.
     */
    private MetricsClient(
            ScheduledExecutorService executorService,
            SessionController sessionController,
            SamplingController samplingController,
            AtomicReference<SourceConfig> sourceConfig,
            BlockingQueue<EventProcessed> eventQueue,
            EventProcessor eventProcessor
    ) {
        this.executorService = executorService;
        this.sessionController = sessionController;
        this.samplingController = samplingController;
        this.sourceConfig = sourceConfig;
        this.eventQueue = eventQueue;
        this.eventProcessor = eventProcessor;
    }

    /**
     * Submit an event to be enqueued and sent to the Event Platform.
     * <p>
     * If stream configs are not yet fetched, the event will be held temporarily in the input
     * buffer (provided there is space to do so).
     * <p>
     * If stream configs are available, the event will be validated and enqueued for submission
     * to the configured event platform intake service.
     * <p>
     * Supplemental metadata is added immediately on intake, regardless of the presence or absence
     * of stream configs, so that the event timestamp is recorded accurately.
     *
     * @param event  event data
     */
    public void submit(Event event) {
        EventProcessed eventProcessed = fromEvent(event);
        addRequiredMetadata(eventProcessed);
        addToEventQueue(eventProcessed);
    }

    /**
     * Construct and submits a Metrics Platform Event from the event name and custom data for each
     * stream specified.
     * <p>
     * The Metrics Platform Event for a stream (S) is constructed by: first initializing the minimum
     * valid event (E) that can be submitted to S; and, second mixing the context attributes requested
     * in the configuration for S into E.
     * <p>
     * The Metrics Platform Event is submitted to a stream (S) if: 1) S is in sample; and 2) the event
     * is filtered due to the filtering rules for S.
     * <p>
     * This particular submitMetricsEvent method accepts unformatted custom data and calls the following
     * submitMetricsEvent method with the custom data properly formatted.
     *
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API">Metrics Platform/Java API</a>
     *
     * @param streamName stream name
     * @param schemaId  schema id
     * @param eventName event name
     * @param customData custom data
     */
    public void submitMetricsEvent(
            String streamName,
            String schemaId,
            String eventName,
            Map<String, Object> customData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, null, customData, null);
    }

    /**
     * Construct and submits a Metrics Platform Event from the schema id, event name, page metadata, and custom data for
     * the stream that is interested in those events.
     *
     * @param streamName stream name
     * @param schemaId  schema id
     * @param eventName event name
     * @param clientData client context data
     * @param customData custom data
     */
    public void submitMetricsEvent(
            String streamName,
            String schemaId,
            String eventName,
            ClientData clientData,
            Map<String, Object> customData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, null);
    }

    /**
     * Construct and submits a Metrics Platform Event from the schema id, stream name, event name, page metadata, custom
     * data, and interaction data for the specified stream.
     *
     * @param streamName stream name
     * @param schemaId  schema id
     * @param eventName event name
     * @param clientData client context data
     * @param customData custom data
     * @param interactionData common data for an interaction schema
     */
    public void submitMetricsEvent(
            String streamName,
            String schemaId,
            String eventName,
            ClientData clientData,
            Map<String, Object> customData,
            InteractionData interactionData
    ) {
        if (streamName == null) {
            log.log(Level.FINE, "No stream has been specified, the submitMetricsEvent event is ignored and dropped.");
            return;
        }

        // If we already have stream configs, then we can pre-validate certain conditions and exclude the event from the queue entirely.
        StreamConfig streamConfig = null;
        if (sourceConfig.get() != null) {
            streamConfig = sourceConfig.get().getStreamConfigByName(streamName);
            if (streamConfig == null) {
                log.log(Level.FINE, "No stream config exists for this stream, the submitMetricsEvent event is ignored and dropped.");
                return;
            }
            if (!samplingController.isInSample(streamConfig)) {
                log.log(Level.FINE, "Not in sample, the submitMetricsEvent event is ignored and dropped.");
                return;
            }
        }

        Event event = new Event(schemaId, streamName, eventName);
        event.setClientData(clientData);

        if (customData != null) {
            event.setCustomData(customData);
        }

        event.setInteractionData(interactionData);

        if (streamConfig != null && streamConfig.hasSampleConfig()) {
            event.setSample(streamConfig.getSampleConfig());
        }

        submit(event);
    }

    /**
     * Submit an interaction event to a stream.
     * <p>
     * An interaction event is meant to represent a basic interaction with some target or some event
     * occurring, e.g. the user (**performer**) tapping/clicking a UI element, or an app notifying the
     * server of its current state.
     *
     * @param streamName stream name
     * @param eventName event name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     *
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API">Metrics Platform/Java API</a>
     */
    public void submitInteraction(
            String streamName,
            String eventName,
            ClientData clientData,
            InteractionData interactionData
    ) {
        submitMetricsEvent(streamName, METRICS_PLATFORM_SCHEMA_BASE, eventName, clientData, null, interactionData);
    }

    /**
     * Submit an interaction event to a stream.
     * <p>
     * See above - takes additional parameters (custom data + custom schema id) to submit an interaction event.
     *
     * @param streamName stream name
     * @param schemaId schema id
     * @param eventName event name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     * @param customData custom data for the interaction
     *
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API">Metrics Platform/Java API</a>
     */
    public void submitInteraction(
            String streamName,
            String schemaId,
            String eventName,
            ClientData clientData,
            InteractionData interactionData,
            Map<String, Object> customData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData);
    }

    /**
     * Submit a click event to a stream.
     *
     * @param streamName stream name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     *
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API">Metrics Platform/Java API</a>
     */
    public void submitClick(
            String streamName,
            ClientData clientData,
            InteractionData interactionData
    ) {
        submitMetricsEvent(streamName, METRICS_PLATFORM_SCHEMA_BASE, "click", clientData, null, interactionData);
    }

    /**
     * Submit a click event to a stream with custom data.
     *
     * @param streamName stream name
     * @param schemaId schema id
     * @param eventName event name
     * @param clientData client context data
     * @param customData custom data for the interaction
     * @param interactionData common data for the base interaction schema
     *
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API">Metrics Platform/Java API</a>
     */
    public void submitClick(
            String streamName,
            String schemaId,
            String eventName,
            ClientData clientData,
            Map<String, Object> customData,
            InteractionData interactionData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData);
    }

    /**
     * Submit a view event to a stream.
     *
     * @param streamName stream name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     */
    public void submitView(
            String streamName,
            ClientData clientData,
            InteractionData interactionData
    ) {
        submitMetricsEvent(streamName, METRICS_PLATFORM_SCHEMA_BASE, "view", clientData, null, interactionData);
    }

    /**
     * Submit a view event to a stream with custom data.
     *
     * @param streamName stream name
     * @param schemaId schema id
     * @param eventName event name
     * @param clientData client context data
     * @param customData custom data for the interaction
     * @param interactionData common data for the base interaction schema
     */
    public void submitView(
            String streamName,
            String schemaId,
            String eventName,
            ClientData clientData,
            Map<String, Object> customData,
            InteractionData interactionData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData);
    }

    /**
     * Convenience method to be called when
     * <a href="https://developer.android.com/guide/components/activities/activity-lifecycle#onpause">
     * the onPause() activity lifecycle callback</a> is called.
     * <p>
     * Touches the session so that we can determine whether it's session has expired if and when the
     * application is resumed.
     */
    public void onAppPause() {
        executorService.schedule(eventProcessor::sendEnqueuedEvents, 0, MILLISECONDS);
        sessionController.touchSession();
    }

    /**
     * Convenience method to be called when
     * <a href="https://developer.android.com/guide/components/activities/activity-lifecycle#onresume">
     * the onResume() activity lifecycle callback</a> is called.
     * <p>
     * Touches the session so that we can determine whether it has expired.
     */
    public void onAppResume() {
        sessionController.touchSession();
    }

    /**
     * Closes the session.
     */
    public void onAppClose() {
        executorService.schedule(eventProcessor::sendEnqueuedEvents, 0, MILLISECONDS);
        sessionController.closeSession();
    }

    /**
     * Begins a new session and touches the session.
     */
    public void resetSession() {
        sessionController.beginSession();
    }

    /**
     * Supplement the outgoing event with additional metadata.
     * These include:
     * - app_session_id: the current session ID
     * - dt: ISO 8601 timestamp
     * - domain: hostname
     *
     * @param event event
     */
    private void addRequiredMetadata(EventProcessed event) {
        event.setPerformerData(
                PerformerData.builderFrom(event.getPerformerData())
                        .sessionId(sessionController.getSessionId())
                        .build());
        event.setTimestamp(DATE_FORMAT.format(now()));
        event.setDomain(event.getClientData().domain);
    }

    /**
     * Append an enriched event to the queue.
     * If the queue is full, we remove the oldest events from the queue to add the current event.
     * Number of attempts to add to the queue is 1/50 of the number queue capacity but at least 10
     *
     * @param event a processed event
     */
    private void addToEventQueue(EventProcessed event) {
        int eventQueueAppendAttempts = max(eventQueue.size() / 50, 10);

        while (!eventQueue.offer(event)) {
            EventProcessed removedEvent = eventQueue.remove();
            if (removedEvent != null) {
                log.log(FINE, removedEvent.getName() + " was dropped so that a newer event could be added to the queue.");
            }
            if (eventQueueAppendAttempts-- <= 0) break;
        }
    }

    public boolean isFullyInitialized() {
        return sourceConfig.get() != null;
    }

    public boolean isEventQueueEmpty() {
        return eventQueue.isEmpty();
    }

    public static Builder builder(ClientData clientData) {
        return new Builder(clientData);
    }

    @NotThreadSafe @ParametersAreNonnullByDefault
    @Setter @Accessors(fluent = true)
    @SuppressWarnings("checkstyle:classfanoutcomplexity") // As the main builder for the application, this class has
                                                          // to fan out to almost everything. We could hide this by
                                                          // using an injection framework (Guice?), but the added
                                                          // dependency is probably not worth it.
    public static final class Builder {

        private final ClientData clientData;
        private final Duration streamConfigFetchRetryDelay = Duration.ofMinutes(1);
        private AtomicReference<SourceConfig> sourceConfigRef = new AtomicReference<>();
        private BlockingQueue<EventProcessed> eventQueue = new LinkedBlockingQueue<>(10);
        private SessionController sessionController = new SessionController();

        private CurationController curationController = new CurationController();

        @Nullable private SamplingController samplingController;

        private OkHttpClient httpClient = new OkHttpClient();
        private URL streamConfigURL = safeURL(ANALYTICS_API_ENDPOINT);
        private Duration streamConfigFetchInitialDelay = Duration.ofSeconds(0);
        private Duration streamConfigFetchInterval = Duration.ofSeconds(30);
        private Duration sendEventsInitialDelay = Duration.ofSeconds(3);
        private Duration sendEventsInterval = Duration.ofSeconds(30);
        private boolean isDebug;
        private Consumer<SourceConfig> sourceConfigConsumer = sourceConfig -> {};
        private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new SimpleThreadFactory());

        @Nullable private SourceConfig sourceConfig;

        public Builder(ClientData clientData) {
            this.clientData = clientData;
        }

        public Builder eventQueueCapacity(int capacity) {
            eventQueue = new LinkedBlockingQueue<>(capacity);
            return this;
        }

        public MetricsClient build() {
            if (sourceConfig != null) sourceConfigRef.set(sourceConfig);

            if (samplingController == null) {
                samplingController = new SamplingController(clientData, sessionController);
            }

            Gson gson = GsonHelper.getGson();

            EventProcessor eventProcessor = new EventProcessor(
                    new ContextController(),
                    curationController,
                    sourceConfigRef,
                    samplingController,
                    new EventSenderDefault(gson, httpClient),
                    eventQueue,
                    isDebug
            );

            MetricsClient metricsClient = new MetricsClient(
                    executorService,
                    sessionController,
                    samplingController,
                    sourceConfigRef,
                    eventQueue,
                    eventProcessor);

            List<Consumer<SourceConfig>> consumers = Stream.of(sourceConfigRef::set, sourceConfigConsumer)
                    .collect(toList());

            StreamConfigFetcher streamConfigFetcher = new StreamConfigFetcher(streamConfigURL, httpClient, gson);

            ConfigFetcherRunnable configFetchRunnable = new ConfigFetcherRunnable(streamConfigFetchInterval,
                    streamConfigFetcher,
                    consumers,
                    executorService, streamConfigFetchRetryDelay);

            startScheduledOperations(eventProcessor, configFetchRunnable, executorService);

            return metricsClient;
        }

        private void startScheduledOperations(
                EventProcessor eventProcessor,
                ConfigFetcherRunnable configFetchRunnable,
                ScheduledExecutorService executorService
        ) {
            executorService.schedule(configFetchRunnable, streamConfigFetchInitialDelay.toMillis(), MILLISECONDS);

            executorService.scheduleAtFixedRate(
                    eventProcessor::sendEnqueuedEvents,
                    sendEventsInitialDelay.toMillis(), sendEventsInterval.toMillis(), MILLISECONDS);
        }

        @SneakyThrows
        private static URL safeURL(String url) {
            return new URL(url);
        }

    }

    private static final class SimpleThreadFactory implements ThreadFactory {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "metrics-client-" + counter.incrementAndGet());
        }
    }

}
