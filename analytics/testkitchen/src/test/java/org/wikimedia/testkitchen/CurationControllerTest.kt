package org.wikimedia.testkitchen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.config.StreamConfigFixtures
import org.wikimedia.testkitchen.context.PerformerData
import org.wikimedia.testkitchen.event.EventFixtures
import java.time.Instant

internal class CurationControllerTest {
    @Test
    fun testEventPasses() {
        assertTrue(curationController.shouldProduceEvent(EventFixtures.getEvent(namespaceName = "Talk", groups = listOf("user", "autoconfirmed"), isLoggedIn = true), streamConfig))
    }

    @Test
    fun testEventFailsWrongPageId() {
        val event = EventFixtures.getEvent(42, "Talk", groups, true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsWrongPageNamespaceText() {
        val event = EventFixtures.getEvent(1, "User", groups, true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsWrongUserGroups() {
        val wrongGroups = listOf("user", "autoconfirmed", "sysop")
        val event = EventFixtures.getEvent(1, "Talk", wrongGroups, true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsNoUserGroups() {
        val event = EventFixtures.getEvent(1, "Talk", emptyList(), true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsNotLoggedIn() {
        val event = EventFixtures.getEvent(1, "Talk", groups, false, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventPassesPerformerRegistrationDtDeserializes() {
        val event = EventFixtures.getEvent(namespaceName = "Talk")
        event.performerData = PerformerData(
            groups = groups,
            isLoggedIn = true,
            registrationDt = Instant.parse("2023-03-01T01:08:30Z")
        )
        assertTrue(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventPassesCurationFilters() {
        val event = EventFixtures.getEvent(1, "Talk", groups, true, "1000+ edits")
        assertTrue(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsEqualsRule() {
        val event = EventFixtures.getEvent(1, "Main", groups, true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsCollectionContainsAnyRule() {
        val event = EventFixtures.getEvent(1, "Talk", listOf("*"), true, "1000+ edits")
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    @Test
    fun testEventFailsCollectionDoesNotContainRule() {
        val event = EventFixtures.getEvent(
            1,
            "Talk",
            listOf("foo", "bar"),
            true,
            "1000+ edits"
        )
        assertFalse(curationController.shouldProduceEvent(event, streamConfig))
    }

    companion object {
        private var streamConfig: StreamConfig = StreamConfigFixtures.streamConfig(
            curationFilter = JsonUtil.decodeFromString(
                "{\"page_id\":{\"less_than\":500,\"not_equals\":42},\"page_namespace_name\":" +
                    "{\"equals\":\"Talk\"},\"performer_is_logged_in\":{\"equals\":true},\"performer_edit_count_bucket\":" +
                    "{\"in\":[\"100-999 edits\",\"1000+ edits\"]},\"performer_groups\":{\"contains_all\":" +
                    "[\"user\",\"autoconfirmed\"],\"does_not_contain\":\"sysop\"}}")
        )

        private val curationController = CurationController()

        private val groups = listOf("user", "autoconfirmed", "steward")
    }
}
