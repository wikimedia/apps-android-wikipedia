package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.config.SourceConfig
import org.wikimedia.metricsplatform.config.StreamConfig
import org.wikimedia.metricsplatform.context.ClientData
import org.wikimedia.metricsplatform.context.InteractionData
import org.wikimedia.metricsplatform.event.Event
import org.wikimedia.metricsplatform.event.EventProcessed
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class MetricsClient private constructor(
    /**
     * Handles logging session management. A new session begins (and a new session ID is created)
     * if the app has been inactive for 15 minutes or more.
     */
    private val sessionController: SessionController,
    /**
     * Evaluates whether events for a given stream are in-sample based on the stream configuration.
     */
    private val samplingController: SamplingController,
    private val sourceConfig: AtomicReference<SourceConfig>,
    eventQueue: BlockingQueue<EventProcessed>,
    eventProcessor: EventProcessor
) {

    private val eventQueue: BlockingQueue<EventProcessed>
    private val eventProcessor: EventProcessor

    /**
     * MetricsClient constructor.
     */
    init {
        this.eventQueue = eventQueue
        this.eventProcessor = eventProcessor
    }

    /**
     * Submit an event to be enqueued and sent to the Event Platform.
     *
     *
     * If stream configs are not yet fetched, the event will be held temporarily in the input
     * buffer (provided there is space to do so).
     *
     *
     * If stream configs are available, the event will be validated and enqueued for submission
     * to the configured event platform intake service.
     *
     *
     * Supplemental metadata is added immediately on intake, regardless of the presence or absence
     * of stream configs, so that the event timestamp is recorded accurately.
     *
     * @param event  event data
     */
    fun submit(event: Event) {
        val eventProcessed = EventProcessed.fromEvent(event)
        addRequiredMetadata(eventProcessed)
        addToEventQueue(eventProcessed)
    }

    /**
     * Construct and submits a Metrics Platform Event from the event name and custom data for each
     * stream specified.
     *
     *
     * The Metrics Platform Event for a stream (S) is constructed by: first initializing the minimum
     * valid event (E) that can be submitted to S; and, second mixing the context attributes requested
     * in the configuration for S into E.
     *
     *
     * The Metrics Platform Event is submitted to a stream (S) if: 1) S is in sample; and 2) the event
     * is filtered due to the filtering rules for S.
     *
     *
     * This particular submitMetricsEvent method accepts unformatted custom data and calls the following
     * submitMetricsEvent method with the custom data properly formatted.
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     *
     *
     * @param streamName stream name
     * @param schemaId  schema id
     * @param eventName event name
     * @param customData custom data
     */
    fun submitMetricsEvent(
        streamName: String,
        schemaId: String,
        eventName: String,
        customData: Map<String, String>?
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, null, customData, null)
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
    fun submitMetricsEvent(
        streamName: String,
        schemaId: String,
        eventName: String,
        clientData: ClientData?,
        customData: Map<String, Any>?,
        interactionData: InteractionData? = null
    ) {
        // If we already have stream configs, then we can pre-validate certain conditions and exclude the event from the queue entirely.
        var streamConfig: StreamConfig? = null
        if (sourceConfig.get() != null) {
            streamConfig = sourceConfig.get().getStreamConfigByName(streamName)
            if (streamConfig == null) {
                //log.log(Level.FINE, "No stream config exists for this stream, the submitMetricsEvent event is ignored and dropped.")
                return
            }
            if (!samplingController.isInSample(streamConfig)) {
                //log.log(Level.FINE, "Not in sample, the submitMetricsEvent event is ignored and dropped.")
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
        if (streamConfig != null && streamConfig.hasSampleConfig()) {
            event.sample = streamConfig.sampleConfig
        }
        submit(event)
    }

    /**
     * Submit an interaction event to a stream.
     *
     *
     * An interaction event is meant to represent a basic interaction with some target or some event
     * occurring, e.g. the user (**performer**) tapping/clicking a UI element, or an app notifying the
     * server of its current state.
     *
     * @param streamName stream name
     * @param eventName event name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitInteraction(
        streamName: String,
        eventName: String,
        clientData: ClientData,
        interactionData: InteractionData?
    ) {
        submitMetricsEvent(
            streamName,
            METRICS_PLATFORM_SCHEMA_BASE,
            eventName,
            clientData,
            null,
            interactionData
        )
    }

    /**
     * Submit an interaction event to a stream.
     *
     *
     * See above - takes additional parameters (custom data + custom schema id) to submit an interaction event.
     *
     * @param streamName stream name
     * @param schemaId schema id
     * @param eventName event name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     * @param customData custom data for the interaction
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitInteraction(
        streamName: String,
        schemaId: String,
        eventName: String,
        clientData: ClientData,
        interactionData: InteractionData?,
        customData: Map<String, Any>?
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData)
    }

    /**
     * Submit a click event to a stream.
     *
     * @param streamName stream name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitClick(
        streamName: String,
        clientData: ClientData,
        interactionData: InteractionData
    ) {
        submitMetricsEvent(
            streamName,
            METRICS_PLATFORM_SCHEMA_BASE,
            "click",
            clientData,
            null,
            interactionData
        )
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
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitClick(
        streamName: String,
        schemaId: String,
        eventName: String,
        clientData: ClientData,
        customData: Map<String, String>?,
        interactionData: InteractionData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData)
    }

    /**
     * Submit a view event to a stream.
     *
     * @param streamName stream name
     * @param clientData client context data
     * @param interactionData common data for the base interaction schema
     */
    fun submitView(
        streamName: String,
        clientData: ClientData,
        interactionData: InteractionData
    ) {
        submitMetricsEvent(
            streamName,
            METRICS_PLATFORM_SCHEMA_BASE,
            "view",
            clientData,
            null,
            interactionData
        )
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
    fun submitView(
        streamName: String,
        schemaId: String,
        eventName: String,
        clientData: ClientData,
        customData: Map<String, String>?,
        interactionData: InteractionData
    ) {
        submitMetricsEvent(streamName, schemaId, eventName, clientData, customData, interactionData)
    }

    /**
     * Convenience method to be called when
     * [
 * the onPause() activity lifecycle callback](https://developer.android.com/guide/components/activities/activity-lifecycle#onpause) is called.
     *
     *
     * Touches the session so that we can determine whether it's session has expired if and when the
     * application is resumed.
     */
    fun onAppPause() {
        sessionController.touchSession()

        //eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Convenience method to be called when
     * [
 * the onResume() activity lifecycle callback](https://developer.android.com/guide/components/activities/activity-lifecycle#onresume) is called.
     *
     *
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

        //eventProcessor.sendEnqueuedEvents()
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
        event.timestamp = DATE_FORMAT.format(ZonedDateTime.now(ZONE_Z))
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

        while (!eventQueue.offer(event)) {
            val removedEvent = eventQueue.remove()
            if (removedEvent != null) {
                //log.log(Level.FINE, removedEvent.name + " was dropped so that a newer event could be added to the queue.")
            }
            if (eventQueueAppendAttempts-- <= 0) break
        }
    }

    val isFullyInitialized get() = sourceConfig.get() != null

    val isEventQueueEmpty get() = eventQueue.isEmpty()

    class Builder(private val clientData: ClientData) {
        private val sourceConfigRef = AtomicReference<SourceConfig>()
        private var eventQueue = LinkedBlockingQueue<EventProcessed>(10)
        private val sessionController = SessionController()

        private val curationController = CurationController()

        private var samplingController: SamplingController? = null

        private var isDebug = false

        private val sourceConfig: SourceConfig? = null

        private var eventSender: EventSender? = null

        fun eventQueueCapacity(capacity: Int): Builder {
            eventQueue = LinkedBlockingQueue(capacity)
            return this
        }

        fun eventSender(eventSender: EventSender): Builder {
            this.eventSender = eventSender
            return this
        }

        fun isDebug(isDebug: Boolean): Builder {
            this.isDebug = isDebug
            return this
        }

        fun build(): MetricsClient {
            if (sourceConfig != null) sourceConfigRef.set(sourceConfig)

            if (samplingController == null) {
                samplingController = SamplingController(clientData, sessionController)
            }

            val eventProcessor = EventProcessor(
                ContextController(),
                curationController,
                sourceConfigRef,
                samplingController!!,
                eventSender!!,
                eventQueue,
                isDebug
            )

            val metricsClient = MetricsClient(
                sessionController,
                samplingController!!,
                sourceConfigRef,
                eventQueue,
                eventProcessor
            )

            return metricsClient
        }
    }

    companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
        val ZONE_Z = ZoneId.of("Z")

        const val METRICS_PLATFORM_LIBRARY_VERSION: String = "2.8"
        const val METRICS_PLATFORM_BASE_VERSION: String = "1.2.2"

        const val METRICS_PLATFORM_SCHEMA_BASE: String = "/analytics/product_metrics/app/base/$METRICS_PLATFORM_BASE_VERSION"

        fun builder(clientData: ClientData): Builder {
            return Builder(clientData)
        }
    }
}
