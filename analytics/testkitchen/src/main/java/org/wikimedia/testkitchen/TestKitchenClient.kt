package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.SourceConfig
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.ClientDataCallback
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.event.Event
import org.wikimedia.testkitchen.instrument.InstrumentImpl
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

class TestKitchenClient(
    eventSender: EventSender,
    sourceConfigInit: SourceConfig? = null,
    val clientDataCallback: ClientDataCallback,
    val queueCapacity: Int = QUEUE_CAPACITY,
    val logger: LogAdapter = DefaultLogAdapterImpl()
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

    private val eventProcessor = EventProcessor(
        ContextController(),
        CurationController(),
        sourceConfig,
        samplingController,
        eventSender,
        queueCapacity,
        logger,
        followCurationRules = false, // TODO: remove when SDK gets instruments dynamically from config.
    )

    fun getInstrument(name: String): InstrumentImpl {
        // TODO: wire up eventual SDK logic to get instruments dynamically from config.
        return InstrumentImpl(name, this)
    }

    fun submitInteraction(
        instrument: InstrumentImpl,
        interactionData: InteractionData? = null,
        pageData: PageData? = null
    ) {
        submitInteraction(SCHEMA_APP_BASE, STREAM_APP_BASE, instrument, interactionData, pageData)
    }

    fun submitInteraction(
        schemaName: String,
        streamName: String,
        instrument: InstrumentImpl,
        interactionData: InteractionData? = null,
        pageData: PageData? = null
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
                pageData = pageData,
                mediawikiData = clientDataCallback.getMediawikiData(),
                performerData = clientDataCallback.getPerformerData()
            ),
            interactionData = interactionData ?: InteractionData(),
            sample = streamConfig?.sampleConfig
        )

        eventProcessor.addToQueue(event)
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

    companion object {
        private val ZONE_Z: ZoneId? = ZoneId.of("Z")
        private const val QUEUE_CAPACITY = 16

        const val BASE_URL = "https://test-kitchen.wikimedia.org/"
        const val LIBRARY_VERSION: String = "1.0.0"
        const val SCHEMA_APP_BASE_VERSION: String = "2.0.0"
        const val SCHEMA_APP_BASE: String = "/analytics/product_metrics/app/base/$SCHEMA_APP_BASE_VERSION"
        const val STREAM_APP_BASE: String = "product_metrics.app_base"
    }
}
