package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.SourceConfig
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.event.Event
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
    val logger: LogAdapter = DefaultLogAdapterImpl(),
    val isDebug: Boolean = false
) {

    private var sourceConfig = AtomicReference<SourceConfig>(sourceConfigInit)

    fun updateSourceConfig(configs: Map<String, StreamConfig>) {
        sourceConfig.set(SourceConfig(configs))
        eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Handles logging session management. A new session begins (and a new session ID is created)
     * if the app has been inactive for a predefined time.
     */
    private val sessionController = SessionController()

    /**
     * Evaluates whether events for a given stream are in-sample based on the stream configuration.
     */
    private val samplingController = SamplingController(clientData, sessionController)

    private val eventQueue: BlockingQueue<Event> = LinkedBlockingQueue(queueCapacity)

    private val eventProcessor: EventProcessor = EventProcessor(
        ContextController(),
        CurationController(),
        sourceConfig,
        samplingController,
        eventSender,
        eventQueue,
        logger,
        isDebug
    )

    fun submitMetricsEvent(
        streamName: String,
        schemaId: String,
        clientData: ClientData? = null,
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

        val event = Event(
            schemaId,
            streamName,
            DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZONE_Z)),
            clientData ?: ClientData(),
            interactionData ?: InteractionData(),
            streamConfig?.sampleConfig
        )
        event.performerData?.let { it.sessionId = sessionController.sessionId }

        addToEventQueue(event)
    }

    /**
     * Submit an interaction event to a stream.
     *
     * @see [Metrics Platform/Java API](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Java_API)
     */
    fun submitInteraction(
        streamName: String,
        schemaId: String = SCHEMA_APP_BASE,
        clientData: ClientData? = null,
        interactionData: InteractionData? = null
    ) {
        submitMetricsEvent(streamName, schemaId, clientData, interactionData)
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

    fun flushEventQueue() {
        eventProcessor.sendEnqueuedEvents()
    }

    /**
     * Append an enriched event to the queue.
     * If the queue is full, we remove the oldest events from the queue to add the current event.
     * Number of attempts to add to the queue is 1/50 of the number queue capacity but at least 10
     *
     * @param event a processed event
     */
    private fun addToEventQueue(event: Event?) {
        var eventQueueAppendAttempts = max(eventQueue.size / 50, 10)

        if (eventQueue.size > queueCapacity / 2) {
            eventProcessor.sendEnqueuedEvents()
        }

        while (!eventQueue.offer(event)) {
            val removedEvent = eventQueue.remove()
            if (removedEvent != null) {
                logger.warn(removedEvent.action + " was dropped so that a newer event could be added to the queue.")
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
