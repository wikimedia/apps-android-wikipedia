package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.config.DestinationEventService
import org.wikimedia.metricsplatform.config.SourceConfig
import org.wikimedia.metricsplatform.config.StreamConfig
import org.wikimedia.metricsplatform.event.EventProcessed
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

class EventProcessor(
    /**
     * Enriches event data with context data requested in the stream configuration.
     */
    private val contextController: ContextController,
    private val curationController: CurationController,
    sourceConfig: AtomicReference<SourceConfig>,
    samplingController: SamplingController,
    eventSender: EventSender,
    eventQueue: BlockingQueue<EventProcessed>,
    isDebug: Boolean
) {
    private val sourceConfig: AtomicReference<SourceConfig>
    private val samplingController: SamplingController
    private val eventQueue: BlockingQueue<EventProcessed>
    private val eventSender: EventSender
    private val isDebug: Boolean

    /**
     * EventProcessor constructor.
     */
    init {
        this.sourceConfig = sourceConfig
        this.samplingController = samplingController
        this.eventSender = eventSender
        this.eventQueue = eventQueue
        this.isDebug = isDebug
    }

    /**
     * Send all events currently in the output buffer.
     *
     *
     * A shallow clone of the output buffer is created and passed to the integration layer for
     * submission by the client. If the event submission succeeds, the events are removed from the
     * output buffer. (Note that the shallow copy created by clone() retains pointers to the original
     * Event objects.) If the event submission fails, a client error is produced, and the events remain
     * in buffer to be retried on the next submission attempt.
     */
    fun sendEnqueuedEvents() {
        val config: SourceConfig? = sourceConfig.get()
        if (config == null) {
            //log.log(Level.FINE, "Configuration is missing, enqueued events are not sent.")
            return
        }

        val pending: ArrayList<EventProcessed> = ArrayList()
        this.eventQueue.drainTo(pending)

        val streamConfigsMap = config.streamConfigsMap

        pending.filter { event -> streamConfigsMap.containsKey(event.stream) }
            .filter { event ->
                val cfg = streamConfigsMap[event.stream]
                if (cfg == null) {
                    false
                } else {
                    cfg.sampleConfig?.let { event.sample = it }
                    samplingController.isInSample(cfg)
                }
            }
            .filter { event -> eventPassesCurationRules(event, streamConfigsMap) }
            .groupBy { event -> destinationEventService(event, streamConfigsMap) }
            .forEach { (destinationEventService, pendingValidEvents) ->
                this.sendEventsToDestination(destinationEventService, pendingValidEvents)
            }
    }

    fun eventPassesCurationRules(
        event: EventProcessed,
        streamConfigMap: Map<String, StreamConfig>
    ): Boolean {
        val streamConfig = streamConfigMap[event.stream]
        if (streamConfig == null) {
            return false
        }
        contextController.enrichEvent(event, streamConfig)
        return curationController.shouldProduceEvent(event, streamConfig)
    }

    private fun destinationEventService(
        event: EventProcessed,
        streamConfigMap: Map<String, StreamConfig>
    ): DestinationEventService {
        val streamConfig = streamConfigMap[event.stream]
        return streamConfig?.destinationEventService ?: DestinationEventService.ANALYTICS
    }

    private fun sendEventsToDestination(
        destinationEventService: DestinationEventService,
        pendingValidEvents: List<EventProcessed>
    ) {
        try {
            eventSender.sendEvents(destinationEventService.getBaseUri(isDebug), pendingValidEvents)
        } catch (e: UnknownHostException) {
            //log.log(Level.WARNING, "Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
            eventQueue.addAll(pendingValidEvents)
        } catch (e: SocketTimeoutException) {
            //log.log(Level.WARNING, "Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
            eventQueue.addAll(pendingValidEvents)
        } catch (e: Exception) {
            //log.log(Level.WARNING, "Failed to send " + pendingValidEvents.size + " events.", e)
        }
    }
}
