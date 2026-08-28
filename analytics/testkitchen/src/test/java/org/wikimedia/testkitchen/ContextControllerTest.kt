package org.wikimedia.testkitchen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikimedia.testkitchen.config.StreamConfigFixtures
import org.wikimedia.testkitchen.context.DataFixtures
import org.wikimedia.testkitchen.event.EventFixtures
import java.time.Instant

internal class ContextControllerTest {
    @Test
    fun testAddRequestedValues() {
        val contextController = ContextController()
        val event = EventFixtures.minimalEvent(clientData = DataFixtures.getTestClientData())
        val streamConfig = StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS["test.stream"]!!
        contextController.enrichEvent(event, streamConfig)

        val agentData = event.agentData!!
        val mediawikiData = event.mediawikiData!!
        val pageData = event.pageData!!
        val performerData = event.performerData!!

        assertEquals(agentData.appFlavor, "devdebug")
        assertEquals(agentData.appInstallId, "ffffffff-ffff-ffff-ffff-ffffffffffff")
        assertEquals(agentData.appTheme, "LIGHT")
        assertEquals(agentData.appVersion, 982734)
        assertEquals(agentData.appVersionName, "2.7.50470-dev-2024-02-14")
        assertEquals(agentData.clientPlatform, "android")
        assertEquals(agentData.clientPlatformFamily, "app")
        assertEquals(agentData.deviceFamily, "Samsung SM-G960F")
        assertEquals(agentData.deviceLanguage, "en")
        assertEquals(agentData.releaseStatus, "dev")

        assertEquals(mediawikiData.database, "enwiki")

        assertEquals(pageData.id, 123)
        assertEquals(pageData.namespaceId, 0)
        assertEquals(pageData.namespaceName, "Main")
        assertEquals(pageData.title, "Test Page Title")
        assertEquals(pageData.revisionId, 321L)
        assertEquals(pageData.contentLanguage, "en")
        assertEquals(pageData.wikidataItemQid, "Q123456")

        assertEquals(performerData.id, 1)
        assertTrue(performerData.isLoggedIn!!)
        assertFalse(performerData.isTemp!!)
        assertEquals(performerData.name, "TestPerformer")
        assertTrue(performerData.groups!!.size == 1 && performerData.groups!!.contains("*"))
        assertEquals(performerData.registrationDt, Instant.parse("2023-03-01T01:08:30Z"))
        assertEquals(performerData.languageGroups, "zh, en")
        assertEquals(performerData.languagePrimary, "zh-tw")
    }
}
