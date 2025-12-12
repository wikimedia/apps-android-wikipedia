package org.wikimedia.testkitchen

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.DataFixtures
import org.wikimedia.testkitchen.event.Event
import java.util.UUID

internal class EventTest {
    @Test
    fun testEventSerialization() {
        val uuid = UUID.randomUUID().toString()
        val event = Event(
            "test/event/1.0.0",
            "test.event",
            "2021-08-27T12:00:00Z",
            DataFixtures.getTestClientData(agentData = AgentData(
                appInstallId = uuid,
                clientPlatform = "android",
                clientPlatformFamily = "app",
                appFlavor = "flamingo",
                appVersion = 123456789,
                appTheme = "giraffe",
                deviceLanguage = "en",
                releaseStatus = "beta"
            )),
            DataFixtures.getTestInteractionData("TestAction")
        )

        val json = JsonUtil.encodeToString(event)

        assertEquals("{" +
                "\"\$schema\":\"test/event/1.0.0\"," +
                "\"dt\":\"2021-08-27T12:00:00Z\"," +
                "\"meta\":{\"stream\":\"test.event\",\"domain\":\"en.wikipedia.org\"}," +
                "\"agent\":{" +
                "\"app_flavor\":\"flamingo\"," +
                "\"app_install_id\":\"$uuid\"," +
                "\"app_theme\":\"giraffe\"," +
                "\"app_version\":123456789," +
                "\"client_platform\":\"android\"," +
                "\"client_platform_family\":\"app\"," +
                "\"device_language\":\"en\"," +
                "\"release_status\":\"beta\"" +
                "}," +
                "\"page\":{" +
                "\"id\":123," +
                "\"title\":\"Test Page Title\"," +
                "\"namespace_id\":0," +
                "\"namespace_name\":\"Main\"," +
                "\"revision_id\":321," +
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
                "\"pageview_id\":\"ffffffffffffffffffff\"," +
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
                "\"funnel_event_sequence_position\":8" +
                "}", json
        )
    }
}
