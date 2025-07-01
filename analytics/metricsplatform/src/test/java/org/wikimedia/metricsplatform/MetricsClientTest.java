package org.wikimedia.metricsplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wikimedia.metricsplatform.MetricsClient.METRICS_PLATFORM_SCHEMA_BASE;
import static org.wikimedia.metricsplatform.config.StreamConfigFixtures.streamConfig;
import static org.wikimedia.metricsplatform.context.DataFixtures.getTestClientData;
import static org.wikimedia.metricsplatform.context.DataFixtures.getTestCustomData;
import static org.wikimedia.metricsplatform.curation.CurationFilterFixtures.curationFilter;
import static org.wikimedia.metricsplatform.event.EventProcessed.fromEvent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wikimedia.metricsplatform.config.SourceConfig;
import org.wikimedia.metricsplatform.config.SourceConfigFixtures;
import org.wikimedia.metricsplatform.config.StreamConfig;
import org.wikimedia.metricsplatform.context.ClientData;
import org.wikimedia.metricsplatform.context.DataFixtures;
import org.wikimedia.metricsplatform.context.InteractionData;
import org.wikimedia.metricsplatform.context.PageData;
import org.wikimedia.metricsplatform.context.PerformerData;
import org.wikimedia.metricsplatform.event.Event;
import org.wikimedia.metricsplatform.event.EventProcessed;

@ExtendWith(MockitoExtension.class)
class MetricsClientTest {

    @Mock private ClientData clientData;
    @Mock private SessionController mockSessionController;
    @Mock private SamplingController mockSamplingController;
    private MetricsClient client;
    private BlockingQueue<EventProcessed> eventQueue;
    private AtomicReference<SourceConfig> sourceConfig;

    @BeforeEach void createEventProcessorMetricsClient() {
        eventQueue = new LinkedBlockingQueue<>(10);
        sourceConfig = new AtomicReference<>(SourceConfigFixtures.getTestSourceConfigMin());
        clientData = DataFixtures.getTestClientData();

        client = MetricsClient.builder(clientData)
                .sessionController(mockSessionController)
                .samplingController(mockSamplingController)
                .sourceConfigRef(sourceConfig)
                .eventQueue(eventQueue)
                .build();
    }

    @Test void testSubmit() {
        Event event = new Event(METRICS_PLATFORM_SCHEMA_BASE, "test_stream", "test_event");
        client.submit(event);
        EventProcessed eventProcessed = eventQueue.peek();
        String stream = eventProcessed.getStream();
        assertThat(stream).isEqualTo("test_stream");
    }

    @Test void testSubmitMetricsEventWithoutClientData() {
        when(mockSamplingController.isInSample(streamConfig(curationFilter()))).thenReturn(true);

        Map<String, Object> customDataMap = getTestCustomData();
        client.submitMetricsEvent("test_stream", METRICS_PLATFORM_SCHEMA_BASE, "test_event", customDataMap);

        assertThat(eventQueue).isNotEmpty();

        EventProcessed queuedEvent = eventQueue.remove();

        // Verify custom data
        assertThat(queuedEvent.getName()).isEqualTo("test_event");
        Map<String, Object> customData = queuedEvent.getCustomData();
        assertThat(customData.get("font_size")).isEqualTo("small");
        assertThat(customData.get("is_full_width")).isEqualTo(true);
        assertThat(customData.get("screen_size")).isEqualTo(1080);

        // Verify that client data is not included
        assertThat(queuedEvent.getAgentData().getClientPlatform()).isNull();
        assertThat(queuedEvent.getPageData().getId()).isNull();
        assertThat(queuedEvent.getPageData().getTitle()).isNull();
        assertThat(queuedEvent.getPageData().getNamespaceId()).isNull();
        assertThat(queuedEvent.getPageData().getNamespaceName()).isNull();
        assertThat(queuedEvent.getPageData().getRevisionId()).isNull();
        assertThat(queuedEvent.getPageData().getWikidataItemQid()).isNull();
        assertThat(queuedEvent.getPageData().getContentLanguage()).isNull();
        assertThat(queuedEvent.getMediawikiData().getDatabase()).isNull();
        assertThat(queuedEvent.getPerformerData().getId()).isNull();
    }

    @Test void testSubmitMetricsEventWithClientData() {
        when(mockSamplingController.isInSample(streamConfig(curationFilter()))).thenReturn(true);

        // Update a few client data members to confirm that the client data parameter during metrics client
        // instantiation gets overridden with the client data sent with the event.
        ClientData clientData = DataFixtures.getTestClientData();
        PageData pageData = PageData.builder()
                .id(108)
                .title("Revised Page Title")
                .namespaceId(0)
                .namespaceName("Main")
                .revisionId(1L)
                .wikidataItemQid("Q123456")
                .contentLanguage("en")
                .build();
        clientData.setPageData(pageData);

        client.submitMetricsEvent("test_stream", METRICS_PLATFORM_SCHEMA_BASE, "test_event", clientData, getTestCustomData());

        assertThat(eventQueue).isNotEmpty();

        EventProcessed queuedEvent = eventQueue.remove();

        // Verify custom data
        assertThat(queuedEvent.getName()).isEqualTo("test_event");
        Map<String, Object> customData = queuedEvent.getCustomData();
        assertThat(customData.get("font_size")).isEqualTo("small");
        assertThat(customData.get("is_full_width")).isEqualTo(true);
        assertThat(customData.get("screen_size")).isEqualTo(1080);

        // Verify client data
        assertThat(queuedEvent.getAgentData().getAppInstallId()).isEqualTo("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThat(queuedEvent.getAgentData().getClientPlatform()).isEqualTo("android");
        assertThat(queuedEvent.getAgentData().getClientPlatformFamily()).isEqualTo("app");

        assertThat(queuedEvent.getPageData().getId()).isEqualTo(108);
        assertThat(queuedEvent.getPageData().getTitle()).isEqualTo("Revised Page Title");
        assertThat(queuedEvent.getPageData().getNamespaceId()).isEqualTo(0);
        assertThat(queuedEvent.getPageData().getNamespaceName()).isEqualTo("Main");
        assertThat(queuedEvent.getPageData().getRevisionId()).isEqualTo(1L);
        assertThat(queuedEvent.getPageData().getWikidataItemQid()).isEqualTo("Q123456");
        assertThat(queuedEvent.getPageData().getContentLanguage()).isEqualTo("en");

        assertThat(queuedEvent.getMediawikiData().getDatabase()).isEqualTo("enwiki");

        assertThat(queuedEvent.getPerformerData().getId()).isEqualTo(1);
        assertThat(queuedEvent.getPerformerData().getName()).isEqualTo("TestPerformer");
        assertThat(queuedEvent.getPerformerData().getIsLoggedIn()).isTrue();
        assertThat(queuedEvent.getPerformerData().getIsTemp()).isFalse();
        assertThat(queuedEvent.getPerformerData().getPageviewId()).isEqualTo("eeeeeeeeeeeeeeeeeeee");
        assertThat(queuedEvent.getPerformerData().getGroups()).contains("*");
        assertThat(queuedEvent.getPerformerData().getLanguageGroups()).isEqualTo("zh, en");
        assertThat(queuedEvent.getPerformerData().getLanguagePrimary()).isEqualTo("zh-tw");
        assertThat(queuedEvent.getPerformerData().getRegistrationDt()).isEqualTo("2023-03-01T01:08:30Z");

        assertThat(queuedEvent.getClientData().domain).isEqualTo("en.wikipedia.org");
    }

    @Test void testSubmitMetricsEventWithInteractionData() {
        when(mockSamplingController.isInSample(streamConfig(curationFilter()))).thenReturn(true);

        ClientData clientData = DataFixtures.getTestClientData();
        Map<String, Object> customDataMap = getTestCustomData();
        InteractionData interactionData = DataFixtures.getTestInteractionData("TestAction");
        client.submitMetricsEvent("test_stream", METRICS_PLATFORM_SCHEMA_BASE, "test_event", clientData, customDataMap, interactionData);

        assertThat(eventQueue).isNotEmpty();

        EventProcessed queuedEvent = eventQueue.remove();

        assertThat(queuedEvent.getAction()).isEqualTo("TestAction");
        assertThat(queuedEvent.getActionSource()).isEqualTo("TestActionSource");
        assertThat(queuedEvent.getActionContext()).isEqualTo("TestActionContext");
        assertThat(queuedEvent.getActionSubtype()).isEqualTo("TestActionSubtype");
        assertThat(queuedEvent.getElementId()).isEqualTo("TestElementId");
        assertThat(queuedEvent.getElementFriendlyName()).isEqualTo("TestElementFriendlyName");
        assertThat(queuedEvent.getFunnelEntryToken()).isEqualTo("TestFunnelEntryToken");
        assertThat(queuedEvent.getFunnelEventSequencePosition()).isEqualTo(8);
    }

    @Test void testSubmitMetricsEventIncludesSample() {
        StreamConfig streamConfig = streamConfig(curationFilter());

        when(mockSamplingController.isInSample(streamConfig)).thenReturn(true);

        Map<String, Object> customDataMap = getTestCustomData();
        client.submitMetricsEvent("test_stream", METRICS_PLATFORM_SCHEMA_BASE, "test_event", customDataMap);

        assertThat(eventQueue).isNotEmpty();

        EventProcessed queuedEvent = eventQueue.remove();

        assertThat(queuedEvent.getSample()).isEqualTo(streamConfig.getSampleConfig());
    }

    @Test void testSubmitWhenEventQueueIsFull() {
        for (int i = 1; i <= 10; i++) {
            Event event = new Event("test_schema" + i, "test_stream" + i, "test_event" + i);
            EventProcessed eventProcessed = fromEvent(event);
            eventQueue.add(eventProcessed);
        }
        EventProcessed oldestEvent = eventQueue.peek();

        Event event11 = new Event("test_schema11", "test_stream11", "test_event11");
        EventProcessed eventProcessed11 = fromEvent(event11);
        client.submit(eventProcessed11);

        assertThat(eventQueue).doesNotContain(oldestEvent);

        Boolean containsNewestEvent = eventQueue.stream().anyMatch(event -> event.getName().equals("test_event11"));
        assertThat(containsNewestEvent).isTrue();
    }

    @Test void testTouchSessionOnAppPause() {
        when(mockSamplingController.isInSample(streamConfig(curationFilter()))).thenReturn(true);
        fillEventQueue();
        assertThat(eventQueue).isNotEmpty();

        client.onAppPause();
        verify(mockSessionController).touchSession();
    }

    @Test void testResumeSessionOnAppResume() {
        client.onAppResume();
        verify(mockSessionController).touchSession();
    }

    @Test void testResetSession() {
        client.resetSession();
        verify(mockSessionController).beginSession();
    }

    @Test void testCloseSessionOnAppClose() {
        when(mockSamplingController.isInSample(streamConfig(curationFilter()))).thenReturn(true);
        fillEventQueue();
        assertThat(eventQueue).isNotEmpty();

        client.onAppClose();
        verify(mockSessionController).closeSession();
    }

    @Test void testAddRequiredMetadata() {
        Event event = new Event("test/event/1.0.0", "test_event", "testEvent");
        assertThat(event.getTimestamp()).isNull();

        client.submit(event);
        EventProcessed queuedEvent = eventQueue.remove();

        assertThat(queuedEvent.getTimestamp()).isNotNull();
        verify(mockSessionController).getSessionId();
    }

    @Test void testPerformerDataLanguageGroups() {
        Event event = new Event("test/event/1.0.0", "test_event", "testEvent");
        ClientData clientData = getTestClientData();
        PerformerData performerData = event.getClientData().getPerformerData();
        clientData.setPerformerData(PerformerData.builderFrom(performerData)
                .languageGroups("[zh-hant, zh-hans, ja, en, zh-yue, ko, fr, de, it, es, pt, da, tr, ru, nl, sv, cs, " +
                        "fi, uk, el, pl, hu, vi, id, ca, mk, sl, ms, tl, avk, lt, sr-el, eu, nb, ceb, als, uz-latn, " +
                        "az, af, nn, et, eo, la, br, jv, io, bg, ro, nrm, pcd, tg-latn, lmo, gl, cy, sq, is, ha, gd, " +
                        "ku-latn, hr, lv, sk, bar, pms, lld, ga, war]")
                .build());
        event.setClientData(clientData);

        client.submit(event);
        EventProcessed queuedEvent = eventQueue.remove();
        assertThat(queuedEvent.getPerformerData().getLanguageGroups().length()).isEqualTo(255);
    }

    private void fillEventQueue() {
        for (int i = 1; i <= 10; i++) {
            client.submitMetricsEvent("test_stream", METRICS_PLATFORM_SCHEMA_BASE, "test_event", getTestCustomData());
        }
    }
}
