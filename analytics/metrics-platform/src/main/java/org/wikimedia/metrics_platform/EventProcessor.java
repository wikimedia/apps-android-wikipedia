package org.wikimedia.metrics_platform;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.annotation.concurrent.ThreadSafe;

import org.wikimedia.metrics_platform.config.DestinationEventService;
import org.wikimedia.metrics_platform.config.SourceConfig;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.event.EventProcessed;

import lombok.extern.java.Log;

@ThreadSafe
@Log
public class EventProcessor {

    /**
     * Enriches event data with context data requested in the stream configuration.
     */
    private final ContextController contextController;
    private final CurationController curationController;

    private final AtomicReference<SourceConfig> sourceConfig;
    private final SamplingController samplingController;
    private final BlockingQueue<EventProcessed> eventQueue;
    private final EventSender eventSender;
    private final boolean isDebug;

    /**
     * EventProcessor constructor.
     */
    public EventProcessor(
            ContextController contextController,
            CurationController curationController,
            AtomicReference<SourceConfig> sourceConfig,
            SamplingController samplingController,
            EventSender eventSender,
            BlockingQueue<EventProcessed> eventQueue,
            boolean isDebug
    ) {
        this.contextController = contextController;
        this.curationController = curationController;
        this.sourceConfig = sourceConfig;
        this.samplingController = samplingController;
        this.eventSender = eventSender;
        this.eventQueue = eventQueue;
        this.isDebug = isDebug;
    }

    /**
     * Send all events currently in the output buffer.
     * <p>
     * A shallow clone of the output buffer is created and passed to the integration layer for
     * submission by the client. If the event submission succeeds, the events are removed from the
     * output buffer. (Note that the shallow copy created by clone() retains pointers to the original
     * Event objects.) If the event submission fails, a client error is produced, and the events remain
     * in buffer to be retried on the next submission attempt.
     */
    public void sendEnqueuedEvents() {
        SourceConfig config = sourceConfig.get();
        if (config == null) {
            log.log(Level.FINE, "Configuration is missing, enqueued events are not sent.");
            return;
        }

        ArrayList<EventProcessed> pending = new ArrayList<>();
        this.eventQueue.drainTo(pending);

        Map<String, StreamConfig> streamConfigsMap = config.getStreamConfigsMap();

        pending.stream()
                .filter(event -> streamConfigsMap.containsKey(event.getStream()))
                .filter(event -> {
                    StreamConfig cfg = streamConfigsMap.get(event.getStream());
                    if (cfg.hasSampleConfig()) {
                        event.setSample(cfg.getSampleConfig());
                    }
                    return samplingController.isInSample(cfg);
                })
                .filter(event -> eventPassesCurationRules(event, streamConfigsMap))
                .collect(groupingBy(event -> destinationEventService(event, streamConfigsMap), toList()))
                .forEach(this::sendEventsToDestination);
    }

    protected boolean eventPassesCurationRules(EventProcessed event, Map<String, StreamConfig> streamConfigMap) {
        StreamConfig streamConfig = streamConfigMap.get(event.getStream());
        contextController.enrichEvent(event, streamConfig);

        return curationController.shouldProduceEvent(event, streamConfig);
    }

    private DestinationEventService destinationEventService(EventProcessed event, Map<String, StreamConfig> streamConfigMap) {
        StreamConfig streamConfig = streamConfigMap.get(event.getStream());
        return streamConfig.getDestinationEventService();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void sendEventsToDestination(
        DestinationEventService destinationEventService,
        List<EventProcessed> pendingValidEvents
    ) {
        try {
            eventSender.sendEvents(destinationEventService.getBaseUri(isDebug), pendingValidEvents);
        } catch (UnknownHostException | SocketTimeoutException e) {
            log.log(Level.WARNING, "Network error while sending " + pendingValidEvents.size() + " events. Adding back to queue.", e);
            eventQueue.addAll(pendingValidEvents);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to send " + pendingValidEvents.size() + " events.", e);
        }
    }
}
