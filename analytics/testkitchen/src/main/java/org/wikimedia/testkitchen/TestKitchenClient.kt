package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.SourceConfig
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.ClientDataCallback
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.event.Event
import org.wikimedia.testkitchen.instrument.InstrumentImpl
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class TestKitchenClient(
    eventSender: EventSender,
    sourceConfigInit: SourceConfig? = null,
    val clientDataCallback: ClientDataCallback,
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
    val sessionController = SessionController()

    /**
     * Evaluates whether events for a given stream are in-sample based on the stream configuration.
     */
    private val samplingController = SamplingController(clientDataCallback, sessionController)

    private val eventQueue: BlockingQueue<Event> = LinkedBlockingQueue(queueCapacity)

    private val eventProcessor = EventProcessor(
        ContextController(),
        CurationController(),
        sourceConfig,
        samplingController,
        eventSender,
        eventQueue,
        logger,
        followCurationRules = false, // TODO: remove when SDK gets instruments dynamically from config.
        isDebug = isDebug
    )

    fun getInstrument(name: String): InstrumentImpl {
        // TODO: wire up eventual SDK logic to get instruments dynamically from config.
        return InstrumentImpl(name, this)
    }

    fun submitInteraction(
        instrument: InstrumentImpl,
        interactionData: InteractionData? = null
    ) {
        submitInteraction(SCHEMA_APP_BASE, STREAM_APP_BASE, instrument, interactionData)
    }

    fun submitInteraction(
        schemaName: String,
        streamName: String,
        instrument: InstrumentImpl,
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
            schemaName,
            streamName,
            DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZONE_Z)),
            instrument = instrument,
            clientData = ClientData(
                agentData = clientDataCallback.getAgentData(),
                pageData = clientDataCallback.getPageData(),
                mediawikiData = clientDataCallback.getMediawikiData(),
                performerData = clientDataCallback.getPerformerData(),
                domain = clientDataCallback.getDomain()
            ),
            interactionData = interactionData ?: InteractionData(),
            sample = streamConfig?.sampleConfig
        )

        addToEventQueue(event)
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

        const val BASE_URL = "https://test-kitchen.wikimedia.org/"
        const val LIBRARY_VERSION: String = "1.0.0"
        const val SCHEMA_APP_BASE_VERSION: String = "2.0.0"
        const val SCHEMA_APP_BASE: String = "/analytics/product_metrics/app/base/$SCHEMA_APP_BASE_VERSION"
        const val STREAM_APP_BASE: String = "product_metrics.app_base"
    }
}
