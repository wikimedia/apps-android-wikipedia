package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.SourceConfig
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.event.Event
import org.wikimedia.testkitchen.event.EventProcessed
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class TestKitchenClient(
    clientData: ClientData,
    eventSender: EventSender,
    sourceConfigInit: SourceConfig? = null,
    val queueCapacity: Int = 100,
    val logger: LogAdapter = LogAdapterImpl()
) {

    private var sourceConfig = AtomicReference<SourceConfig>(sourceConfigInit)

    fun updateSourceConfig(configs: Map<String, StreamConfig>) {
        sourceConfig.set(SourceConfig(configs))
        eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Handles logging session management. A new session begins (and a new session ID is created)
     * if the app has been inactive for 15 minutes or more.
     */
    private val sessionController = SessionController()

    /**
     * Evaluates whether events for a given stream are in-sample based on the stream configuration.
     */
    private val samplingController = SamplingController(clientData, sessionController)

    private val eventQueue: BlockingQueue<EventProcessed> = LinkedBlockingQueue(queueCapacity)

    private val eventProcessor: EventProcessor = EventProcessor(
        ContextController(),
        CurationController(),
        sourceConfig,
        samplingController,
        eventSender,
        eventQueue,
        logger
    )

    /**
     * Submit an event to be enqueued and sent to the Event Platform.
     *
     * If stream configs are not yet fetched, the event will be held temporarily in the input
     * buffer (provided there is space to do so).
     *
     * If stream configs are available, the event will be validated and enqueued for submission
     * to the configured event platform intake service.
     *
     * Supplemental metadata is added immediately on intake, regardless of the presence or absence
     * of stream configs, so that the event timestamp is recorded accurately.
     */
    fun submit(event: Event) {
        val eventProcessed = EventProcessed.fromEvent(event)
        addRequiredMetadata(eventProcessed)
        addToEventQueue(eventProcessed)
    }

    /**
     * Construct and submits a Metrics Platform Event from the schema id, event name, page metadata, and custom data for
     * the stream that is interested in those events.
     */
    fun submitMetricsEvent(
        streamName: String,
        schemaId: String,
        eventName: String,
        clientData: ClientData? = null,
        customData: Map<String, Any>? = null,
        interactionData: InteractionData? = null
    ) {
        // If we already have stream configs, then we can pre-validate certain conditions and exclude the event from the queue entirely.
        var streamConfig: StreamConfig? = null
        if (sourceConfig.get() != null) {
            streamConfig = sourceConfig.get().getStreamConfigByName(streamName)
            if (streamConfig == null) {
                logger.info("No stream config exists for this stream, the submitMetricsEvent event is ignored and dropped.")
                return
            }
            if (!samplingController.isInSample(streamConfig)) {
                logger.info("Not in sample, the submitMetricsEvent event is ignored and dropped.")
                return
            }
        }

        val event = Event(streamName)
        event.schema = schemaId
        event.name = eventName
        if (clientData != null) {
            event.clientData = clientData
        }
        if (customData != null) {
            event.customData = customData.mapValues { it.value.toString() }
        }
        if (interactionData != null) {
            event.interactionData = interactionData
        }
        if (streamConfig?.sampleConfig != null) {
            event.sample = streamConfig.sampleConfig
        }
        submit(event)
    }

    /**
     * Submit an interaction event to a stream.
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitInteraction(
        streamName: String,
        eventName: String,
        schemaId: String = SCHEMA_APP_BASE,
        clientData: ClientData? = null,
        interactionData: InteractionData? = null,
        customData: Map<String, Any>? = null
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData)
    }

    /**
     * Touches the session so that we can determine whether its session has expired if and when the
     * application is resumed.
     */
    fun onAppPause() {
        sessionController.touchSession()
        eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Touches the session so that we can determine whether it has expired.
     */
    fun onAppResume() {
        sessionController.touchSession()
    }

    /**
     * Closes the session.
     */
    fun onAppClose() {
        sessionController.closeSession()
        eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Begins a new session and touches the session.
     */
    fun resetSession() {
        sessionController.beginSession()
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
    private fun addRequiredMetadata(event: EventProcessed) {
        event.performerData?.let { it.sessionId = sessionController.sessionId }
        event.timestamp = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZONE_Z))
        event.setDomain(event.clientData.domain)
    }

    /**
     * Append an enriched event to the queue.
     * If the queue is full, we remove the oldest events from the queue to add the current event.
     * Number of attempts to add to the queue is 1/50 of the number queue capacity but at least 10
     *
     * @param event a processed event
     */
    private fun addToEventQueue(event: EventProcessed?) {
        var eventQueueAppendAttempts = max(eventQueue.size / 50, 10)

        if (eventQueue.size > queueCapacity / 2) {
            eventProcessor.sendEnqueuedEvents()
        }

        while (!eventQueue.offer(event)) {
            val removedEvent = eventQueue.remove()
            if (removedEvent != null) {
                logger.warn(removedEvent.name + " was dropped so that a newer event could be added to the queue.")
            }
            if (eventQueueAppendAttempts-- <= 0) break
        }
    }

    companion object {
        private val ZONE_Z: ZoneId? = ZoneId.of("Z")

        const val LIBRARY_VERSION: String = "1.0.0"
        const val SCHEMA_APP_BASE_VERSION: String = "1.6.0"
        const val SCHEMA_APP_BASE: String = "/analytics/product_metrics/app/base/$SCHEMA_APP_BASE_VERSION"
    }
}
