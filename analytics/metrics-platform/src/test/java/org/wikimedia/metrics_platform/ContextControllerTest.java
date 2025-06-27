package org.wikimedia.metrics_platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metrics_platform.event.EventProcessed.fromEvent;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.config.StreamConfigFixtures;
import org.wikimedia.metrics_platform.context.AgentData;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.DataFixtures;
import org.wikimedia.metrics_platform.context.MediawikiData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;
import org.wikimedia.metrics_platform.event.Event;
import org.wikimedia.metrics_platform.event.EventProcessed;

class ContextControllerTest {

    @Test void testAddRequestedValues() {
        ContextController contextController = new ContextController();
        Event eventBasic = new Event("test/event", "test.stream", "testEvent");
        EventProcessed event = fromEvent(eventBasic);
        ClientData clientDataSample = DataFixtures.getTestClientData();
        event.setClientData(clientDataSample);
        StreamConfig streamConfig = StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS.get("test.stream");
        contextController.enrichEvent(event, streamConfig);

        AgentData agentData = event.getAgentData();
        MediawikiData mediawikiData = event.getMediawikiData();
        PageData pageData = event.getPageData();
        PerformerData performerData = event.getPerformerData();

        assertThat(agentData.getAppFlavor()).isEqualTo("devdebug");
        assertThat(agentData.getAppInstallId()).isEqualTo("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThat(agentData.getAppTheme()).isEqualTo("LIGHT");
        assertThat(agentData.getAppVersion()).isEqualTo(982734);
        assertThat(agentData.getAppVersionName()).isEqualTo("2.7.50470-dev-2024-02-14");
        assertThat(agentData.getClientPlatform()).isEqualTo("android");
        assertThat(agentData.getClientPlatformFamily()).isEqualTo("app");
        assertThat(agentData.getDeviceFamily()).isEqualTo("Samsung SM-G960F");
        assertThat(agentData.getDeviceLanguage()).isEqualTo("en");
        assertThat(agentData.getReleaseStatus()).isEqualTo("dev");

        assertThat(mediawikiData.getDatabase()).isEqualTo("enwiki");

        assertThat(pageData.getId()).isEqualTo(1);
        assertThat(pageData.getNamespaceId()).isEqualTo(0);
        assertThat(pageData.getNamespaceName()).isEqualTo("Main");
        assertThat(pageData.getTitle()).isEqualTo("Test Page Title");
        assertThat(pageData.getRevisionId()).isEqualTo(1L);
        assertThat(pageData.getContentLanguage()).isEqualTo("en");
        assertThat(pageData.getWikidataItemQid()).isEqualTo("Q123456");

        assertThat(performerData.getId()).isEqualTo(1);
        assertThat(performerData.getIsLoggedIn()).isTrue();
        assertThat(performerData.getIsTemp()).isFalse();
        assertThat(performerData.getName()).isEqualTo("TestPerformer");
        assertThat(performerData.getGroups()).containsExactly("*");
        assertThat(performerData.getRegistrationDt()).isEqualTo("2023-03-01T01:08:30Z");
        assertThat(performerData.getLanguageGroups()).isEqualTo("zh, en");
        assertThat(performerData.getLanguagePrimary()).isEqualTo("zh-tw");
    }
}
