package org.wikimedia.metricsplatform;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wikimedia.metricsplatform.event.EventFixtures.minimalEventProcessed;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wikimedia.metricsplatform.config.SourceConfig;
import org.wikimedia.metricsplatform.config.SourceConfigFixtures;
import org.wikimedia.metricsplatform.config.StreamConfig;
import org.wikimedia.metricsplatform.context.ClientData;
import org.wikimedia.metricsplatform.event.EventProcessed;

@ExtendWith(MockitoExtension.class)
class EventProcessorTest {
    @Mock private EventSender mockEventSender;
    @Mock private CurationController mockCurationController;
    private final AtomicReference<SourceConfig> sourceConfig = new AtomicReference<>();
    private final BlockingQueue<EventProcessed> eventQueue = new LinkedBlockingQueue<>(10);
    private final ClientData consistencyTestClientData = ConsistencyITClientData.createConsistencyTestClientData();
    private final SamplingController samplingController = new SamplingController(consistencyTestClientData, new SessionController());
    private EventProcessor eventProcessor;

    @BeforeEach void clearEventQueue() {
        eventQueue.clear();
    }

    @BeforeEach void createEventProcessor() {
        sourceConfig.set(SourceConfigFixtures.getTestSourceConfigMax());

        eventProcessor = new EventProcessor(
                new ContextController(),
                mockCurationController,
                sourceConfig,
                samplingController,
                mockEventSender,
                eventQueue,
                false
        );
    }

    @Test void enqueuedEventsAreSent() throws IOException {
        whenEventsArePassingCurationFilter();

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();
        verify(mockEventSender).sendEvents(any(URL.class), anyCollection());
    }

    @Test void eventsNotPassingCurationFiltersAreDropped() throws IOException {
        whenEventsAreNotPassingCurationFilter();

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        verify(mockEventSender, never()).sendEvents(any(URL.class), anyCollection());
        assertThat(eventQueue).isEmpty();
    }

    @Test void eventsAreRemovedFromQueueOnceSent() {
        whenEventsArePassingCurationFilter();

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        assertThat(eventQueue).isEmpty();
    }

    @Test void eventsRemainInOutputBufferOnFailure() throws IOException {
        whenEventsArePassingCurationFilter();
        doThrow(UnknownHostException.class).when(mockEventSender).sendEvents(any(URL.class), anyCollection());

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        assertThat(eventQueue).isNotEmpty();
    }

    @Test void eventsAreEnrichedBeforeBeingSent() throws IOException {
        whenEventsArePassingCurationFilter();

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventProcessed>> eventCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(mockEventSender).sendEvents(any(URL.class), eventCaptor.capture());

        EventProcessed sentEvent = eventCaptor.getValue().iterator().next();

        // Verify client data based on minimum provided values in StreamConfigFixtures.
        assertThat(sentEvent.getAgentData().getClientPlatform()).isEqualTo("android");
        assertThat(sentEvent.getAgentData().getClientPlatformFamily()).isEqualTo("app");
        assertThat(sentEvent.getPageData().getTitle()).isEqualTo("Test Page Title");
        assertThat(sentEvent.getMediawikiData().getDatabase()).isEqualTo("enwiki");
        assertThat(sentEvent.getPerformerData().getSessionId()).isEqualTo("eeeeeeeeeeeeeeeeeeee");
    }

    @Test void eventsNotSentWhenFetchStreamConfigFails() {
        sourceConfig.set(null);

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        assertThat(eventQueue).isNotEmpty();
    }

    @Test void testSentEventsHaveClientData() throws IOException {
        whenEventsArePassingCurationFilter();

        eventQueue.offer(minimalEventProcessed());
        eventProcessor.sendEnqueuedEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventProcessed>> eventCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(mockEventSender).sendEvents(any(URL.class), eventCaptor.capture());

        EventProcessed sentEvent = eventCaptor.getValue().iterator().next();

        // Verify client data based on extended provided values in StreamConfigFixtures.
        assertThat(sentEvent.getAgentData().getAppFlavor()).isEqualTo("devdebug");
        assertThat(sentEvent.getAgentData().getAppInstallId()).isEqualTo("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThat(sentEvent.getAgentData().getAppTheme()).isEqualTo("LIGHT");
        assertThat(sentEvent.getAgentData().getAppVersion()).isEqualTo(982734);
        assertThat(sentEvent.getAgentData().getAppVersionName()).isEqualTo("2.7.50470-dev-2024-02-14");
        assertThat(sentEvent.getAgentData().getClientPlatform()).isEqualTo("android");
        assertThat(sentEvent.getAgentData().getClientPlatformFamily()).isEqualTo("app");
        assertThat(sentEvent.getAgentData().getDeviceLanguage()).isEqualTo("en");
        assertThat(sentEvent.getAgentData().getReleaseStatus()).isEqualTo("dev");

        assertThat(sentEvent.getPageData().getId()).isEqualTo(1);
        assertThat(sentEvent.getPageData().getNamespaceId()).isEqualTo(0);
        assertThat(sentEvent.getPageData().getWikidataItemQid()).isEqualTo("Q123456");

        assertThat(sentEvent.getPerformerData().getPageviewId()).isEqualTo("eeeeeeeeeeeeeeeeeeee");
        assertThat(sentEvent.getPerformerData().getLanguageGroups()).isEqualTo("zh, en");
        assertThat(sentEvent.getPerformerData().getLanguagePrimary()).isEqualTo("zh-tw");
    }

    private void whenEventsArePassingCurationFilter() {
        when(
            mockCurationController.shouldProduceEvent(
                any(EventProcessed.class),
                any(StreamConfig.class)
            )
        ).thenReturn(TRUE);
    }

    private void whenEventsAreNotPassingCurationFilter() {
        when(
            mockCurationController.shouldProduceEvent(
                any(EventProcessed.class),
                any(StreamConfig.class)
            )
        ).thenReturn(FALSE);
    }
}
