package org.wikimedia.testkitchen

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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class EventProcessor(
    private val contextController: ContextController,
    private val curationController: CurationController,
    private val sourceConfig: AtomicReference<SourceConfig>,
    private val samplingController: SamplingController,
    private val eventSender: EventSender,
    private val queueCapacity: Int,
    private val logger: LogAdapter,
    private val followCurationRules: Boolean = true
) {
    private val eventQueue = LinkedBlockingQueue<Event>(queueCapacity)

    /**
     * Append an enriched event to the queue.
     * If the queue is full, we remove the oldest events from the queue to add the current event.
     * Number of attempts to add to the queue is 1/50 of the number queue capacity but at least 10
     *
     * @param event a processed event
     * @param sendIfHalfFull whether to send the event if the queue is half-full or greater.
     */
    fun addToQueue(event: Event?, evictIfFull: Boolean = true, sendIfHalfFull: Boolean = true) {
        var eventQueueAppendAttempts = max(eventQueue.size / 50, 10)

        synchronized(eventQueue) {
            while (!eventQueue.offer(event)) {
                if (!evictIfFull) break
                val removedEvent = eventQueue.remove()
                if (removedEvent != null) {
                    logger.warn(removedEvent.action + " was dropped so that a newer event could be added to the queue.")
                }
                if (eventQueueAppendAttempts-- <= 0) break
            }
        }
        if (sendIfHalfFull && eventQueue.size > queueCapacity / 2) {
            sendEnqueuedEvents()
        }
    }

    private fun reAddToQueue(events: List<Event>) {
        events.forEach { addToQueue(it, evictIfFull = false, sendIfHalfFull = false) }
    }

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
                if (followCurationRules) eventPassesCurationRules(event, streamConfigsMap) else true
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
                    eventSender.sendEvents(destinationEventService, pendingValidEvents)
                } catch (e: UnknownHostException) {
                    logger.error("Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
                    reAddToQueue(pendingValidEvents)
                } catch (e: SocketTimeoutException) {
                    logger.error("Network error while sending " + pendingValidEvents.size + " events. Adding back to queue.", e)
                    reAddToQueue(pendingValidEvents)
                } catch (e: Exception) {
                    logger.error("Failed to send " + pendingValidEvents.size + " events.", e)
                }
            }
        }
    }
}
