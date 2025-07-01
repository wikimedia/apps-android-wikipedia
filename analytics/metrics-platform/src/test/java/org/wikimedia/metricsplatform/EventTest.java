package org.wikimedia.metricsplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metricsplatform.MetricsClient.DATE_FORMAT;
import static org.wikimedia.metricsplatform.event.EventProcessed.fromEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.context.AgentData;
import org.wikimedia.metricsplatform.context.ClientData;
import org.wikimedia.metricsplatform.context.DataFixtures;
import org.wikimedia.metricsplatform.event.Event;
import org.wikimedia.metricsplatform.event.EventProcessed;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.google.gson.Gson;

class EventTest {

    @Test void testEvent() {
        Event eventBasic = new Event("test/event/1.0.0", "test.event", "testEvent");
        EventProcessed event = fromEvent(eventBasic);
        String timestamp = DATE_FORMAT.format(Instant.EPOCH);
        event.setTimestamp(timestamp);

        assertThat(event.getStream()).isEqualTo("test.event");
        assertThat(event.getTimestamp()).isEqualTo("1970-01-01T00:00:00Z");
    }

    @Test void testEventSerialization() {
        String uuid = UUID.randomUUID().toString();
        Event eventBasic = new Event("test/event/1.0.0", "test.event", "testEvent");
        EventProcessed event = fromEvent(eventBasic);

        event.setTimestamp("2021-08-27T12:00:00Z");

        event.setClientData(DataFixtures.getTestClientData());
        ClientData clientData = DataFixtures.getTestClientData();
        clientData.setAgentData(
                AgentData.builder()
                        .appFlavor("flamingo")
                        .appInstallId(uuid)
                        .appTheme("giraffe")
                        .appVersion(123456789)
                        .clientPlatform("android")
                        .clientPlatformFamily("app")
                        .deviceLanguage("en")
                        .releaseStatus("beta")
                        .build()
        );
        event.setClientData(clientData);

        assertThat(event.getStream()).isEqualTo("test.event");
        assertThat(event.getSchema()).isEqualTo("test/event/1.0.0");
        assertThat(event.getName()).isEqualTo("testEvent");
        assertThat(event.getAgentData().getAppFlavor()).isEqualTo("flamingo");
        assertThat(event.getAgentData().getAppInstallId()).isEqualTo(uuid);
        assertThat(event.getAgentData().getAppTheme()).isEqualTo("giraffe");
        assertThat(event.getAgentData().getAppVersion()).isEqualTo(123456789);
        assertThat(event.getTimestamp()).isEqualTo("2021-08-27T12:00:00Z");
        assertThat(event.getAgentData().getClientPlatform()).isEqualTo("android");
        assertThat(event.getAgentData().getClientPlatformFamily()).isEqualTo("app");
        assertThat(event.getAgentData().getDeviceLanguage()).isEqualTo("en");
        assertThat(event.getAgentData().getReleaseStatus()).isEqualTo("beta");

        assertThat(event.getPageData().getId()).isEqualTo(1);
        assertThat(event.getPageData().getTitle()).isEqualTo("Test Page Title");
        assertThat(event.getPageData().getNamespaceId()).isEqualTo(0);
        assertThat(event.getPageData().getNamespaceName()).isEqualTo("Main");
        assertThat(event.getPageData().getRevisionId()).isEqualTo(1);
        assertThat(event.getPageData().getWikidataItemQid()).isEqualTo("Q123456");
        assertThat(event.getPageData().getContentLanguage()).isEqualTo("en");

        assertThat(event.getMediawikiData().getDatabase()).isEqualTo("enwiki");

        assertThat(event.getPerformerData().getId()).isEqualTo(1);
        assertThat(event.getPerformerData().getName()).isEqualTo("TestPerformer");
        assertThat(event.getPerformerData().getIsLoggedIn()).isTrue();
        assertThat(event.getPerformerData().getIsTemp()).isFalse();
        assertThat(event.getPerformerData().getSessionId()).isEqualTo("eeeeeeeeeeeeeeeeeeee");
        assertThat(event.getPerformerData().getPageviewId()).isEqualTo("eeeeeeeeeeeeeeeeeeee");
        assertThat(event.getPerformerData().getGroups()).isEqualTo(Collections.singletonList("*"));
        assertThat(event.getPerformerData().getLanguageGroups()).isEqualTo("zh, en");
        assertThat(event.getPerformerData().getLanguagePrimary()).isEqualTo("zh-tw");
        assertThat(event.getPerformerData().getRegistrationDt()).isEqualTo("2023-03-01T01:08:30Z");

        event.setInteractionData(DataFixtures.getTestInteractionData("TestAction"));

        assertThat(event.getAction()).isEqualTo("TestAction");
        assertThat(event.getActionSource()).isEqualTo("TestActionSource");
        assertThat(event.getActionContext()).isEqualTo("TestActionContext");
        assertThat(event.getActionSubtype()).isEqualTo("TestActionSubtype");
        assertThat(event.getElementId()).isEqualTo("TestElementId");
        assertThat(event.getElementFriendlyName()).isEqualTo("TestElementFriendlyName");
        assertThat(event.getFunnelEntryToken()).isEqualTo("TestFunnelEntryToken");
        assertThat(event.getFunnelEventSequencePosition()).isEqualTo(8);

        Gson gson = GsonHelper.getGson();
        String json = gson.toJson(event);
        assertThat(json).isEqualTo(String.format(Locale.ROOT,
                "{" +
                        "\"agent\":{" +
                        "\"app_flavor\":\"flamingo\"," +
                        "\"app_install_id\":\"%s\"," +
                        "\"app_theme\":\"giraffe\"," +
                        "\"app_version\":123456789," +
                        "\"client_platform\":\"android\"," +
                        "\"client_platform_family\":\"app\"," +
                        "\"device_language\":\"en\"," +
                        "\"release_status\":\"beta\"" +
                        "}," +
                        "\"page\":{" +
                        "\"id\":1," +
                        "\"title\":\"Test Page Title\"," +
                        "\"namespace_id\":0," +
                        "\"namespace_name\":\"Main\"," +
                        "\"revision_id\":1," +
                        "\"wikidata_qid\":\"Q123456\"," +
                        "\"content_language\":\"en\"" +
                        "}," +
                        "\"mediawiki\":{" +
                        "\"database\":\"enwiki\"" +
                        "}," +
                        "\"performer\":{" +
                        "\"id\":1," +
                        "\"name\":\"TestPerformer\"," +
                        "\"is_logged_in\":true," +
                        "\"is_temp\":false," +
                        "\"session_id\":\"eeeeeeeeeeeeeeeeeeee\"," +
                        "\"pageview_id\":\"eeeeeeeeeeeeeeeeeeee\"," +
                        "\"groups\":[\"*\"]," +
                        "\"language_groups\":\"zh, en\"," +
                        "\"language_primary\":\"zh-tw\"," +
                        "\"registration_dt\":\"2023-03-01T01:08:30Z\"" +
                        "}," +
                        "\"action\":\"TestAction\"," +
                        "\"action_subtype\":\"TestActionSubtype\"," +
                        "\"action_source\":\"TestActionSource\"," +
                        "\"action_context\":\"TestActionContext\"," +
                        "\"element_id\":\"TestElementId\"," +
                        "\"element_friendly_name\":\"TestElementFriendlyName\"," +
                        "\"funnel_entry_token\":\"TestFunnelEntryToken\"," +
                        "\"funnel_event_sequence_position\":8," +
                        "\"$schema\":\"test/event/1.0.0\"," +
                        "\"dt\":\"2021-08-27T12:00:00Z\"," +
                        "\"meta\":{\"stream\":\"test.event\"}" +
                        "}", uuid, uuid));
    }

}
