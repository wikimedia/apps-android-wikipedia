package org.wikimedia.testkitchen

import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.config.SourceConfig
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.event.Event
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

class EventProcessor(
    private val contextController: ContextController,
    private val curationController: CurationController,
    private val sourceConfig: AtomicReference<SourceConfig>,
    private val samplingController: SamplingController,
    private val eventSender: EventSender,
    private val eventQueue: BlockingQueue<Event>,
    private val logger: LogAdapter,
    private val isDebug: Boolean = false
) {

    /**
     * Send all events currently in the output buffer.
     *
     * If the event submission succeeds, the events are removed from the output buffer. If the event
     * submission fails, a client error is produced, and the events remain in the buffer to be
     * retried on the next submission attempt.
     */
    fun sendEnqueuedEvents() {
        val config: SourceConfig? = sourceConfig.get()
        if (config == null) {
            logger.warn("Configuration is missing, enqueued events are not sent.")
            return
        }

        val pending = mutableListOf<Event>()
        synchronized(eventQueue) {
            eventQueue.drainTo(pending)
        }

        val streamConfigsMap = config.streamConfigs

        pending.filter { event -> streamConfigsMap.containsKey(event.meta.stream) }
            .filter { event ->
                val cfg = streamConfigsMap[event.meta.stream]
                if (cfg == null) {
                    false
                } else {
                    cfg.sampleConfig?.let { event.sample = it }
                    samplingController.isInSample(cfg)
                }
            }
            .filter { event ->
                eventPassesCurationRules(event, streamConfigsMap)
            }
            .groupBy { event ->
                destinationEventService(event, streamConfigsMap)
            }
            .forEach { (destinationEventService, pendingValidEvents) ->
                sendEventsToDestination(destinationEventService, pendingValidEvents)
            }
    }

    fun eventPassesCurationRules(
        event: Event,
        streamConfigMap: Map<String, StreamConfig>
    ): Boolean {
        val streamConfig = streamConfigMap[event.meta.stream] ?: return false
        contextController.enrichEvent(event, streamConfig)
        return curationController.shouldProduceEvent(event, streamConfig)
    }

    private fun destinationEventService(
        event: Event,
        streamConfigMap: Map<String, StreamConfig>
    ): DestinationEventService {
        val streamConfig = streamConfigMap[event.meta.stream]
        return streamConfig?.destinationEventService ?: DestinationEventService.ANALYTICS
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendEventsToDestination(
        destinationEventService: DestinationEventService,
        pendingValidEvents: List<Event>
    ) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    eventSender.sendEvents((destinationEventService.baseUri + "/v1/events" + (if (!isDebug) "?hasty=true" else "")).toUri(), pendingValidEvents)
                } catch (e: UnknownHostException) {
                    logger.error("Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
                    eventQueue.addAll(pendingValidEvents)
                } catch (e: SocketTimeoutException) {
                    logger.error("Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
                    eventQueue.addAll(pendingValidEvents)
                } catch (e: Exception) {
                    logger.error("Failed to send " + pendingValidEvents.size + " events.", e)
                }
            }
        }
    }
}
