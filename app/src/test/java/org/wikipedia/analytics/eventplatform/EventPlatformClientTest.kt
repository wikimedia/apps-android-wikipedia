package org.wikipedia.analytics.eventplatform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.config.StreamConfigCollection
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient.SamplingController
import org.wikipedia.dataclient.ServiceFactory.getAnalyticsRest
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class EventPlatformClientTest {
    @Before
    fun reset() {
        EventPlatformClient.AssociationController.beginNewSession()

        // Set app install ID
        WikipediaApp.instance.appInstallID
    }

    @Test
    fun testGenerateRandomId() {
        val id = EventPlatformClient.AssociationController.pageViewId
        assertEquals(20, id.length)
    }

    @Test
    fun testGetStream() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "test"
        })
        assertNotNull(EventPlatformClient.getStreamConfig("test"))
        assertNull(EventPlatformClient.getStreamConfig("key.does.not.exist"))
    }

    @Test
    fun testEventSerialization() {
        val event = TestAppsEvent("test")
        val serialized = JsonUtil.encodeToString(event)!!
        assertTrue(serialized.contains("dt"))
        assertTrue(serialized.contains("app_session_id"))
        assertTrue(serialized.contains("app_install_id"))
    }

    @Test
    fun testAssociationControllerGetPageViewId() {
        val pageViewId = EventPlatformClient.AssociationController.pageViewId
        assertEquals(20, pageViewId.length)
    }

    @Test
    fun testAssociationControllerGetSessionId() {
        val sessionId = EventPlatformClient.AssociationController.sessionId
        assertEquals(20, sessionId.length)
        val persistentSessionId = Prefs.eventPlatformSessionId
        assertNotNull(persistentSessionId)
        assertEquals(20, persistentSessionId!!.length)
    }

    @Test
    fun testNeverInSampleIfNoStreamConfig() {
        assertFalse(SamplingController.isInSample(TestEvent("not-configured")))
    }

    @Test
    fun testAlwaysInSampleIfStreamConfiguredButNoSamplingConfig() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "configured"
        })
        assertTrue(SamplingController.isInSample(TestEvent("configured")))
    }

    @Test
    fun testAlwaysInSample() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "alwaysInSample"
            it.sampleConfig = SampleConfig(1.0)
        })
        assertTrue(SamplingController.isInSample(TestEvent("alwaysInSample")))
    }

    @Test
    fun testNeverInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig().also {
                it.streamName = "neverInSample"
                it.sampleConfig = SampleConfig(0.0)
            }
        )
        assertFalse(SamplingController.isInSample(TestEvent("neverInSample")))
    }

    @Test
    fun testSamplingControllerGetSamplingValue() {
        val deviceVal = SamplingController.getSamplingValue(SampleConfig.UNIT_DEVICE)
        assertTrue(deviceVal >= 0.0)
        assertTrue(deviceVal <= 1.0)
        val pageViewVal = SamplingController.getSamplingValue(SampleConfig.UNIT_PAGEVIEW)
        assertTrue(pageViewVal >= 0.0)
        assertTrue(pageViewVal <= 1.0)
        val sessionVal = SamplingController.getSamplingValue(SampleConfig.UNIT_SESSION)
        assertTrue(sessionVal >= 0.0)
        assertTrue(sessionVal <= 1.0)
    }

    @Test
    fun testSamplingControllerGetSamplingId() {
        assertNotNull(SamplingController.getSamplingId(SampleConfig.UNIT_DEVICE))
        assertNotNull(SamplingController.getSamplingId(SampleConfig.UNIT_PAGEVIEW))
        assertNotNull(SamplingController.getSamplingId(SampleConfig.UNIT_SESSION))
    }

    @Test
    fun testGetEventService() {
        val streamConfig = StreamConfig().also {
            it.streamName = "test"
            it.destinationEventService = DestinationEventService.LOGGING
        }
        assertNotNull(getAnalyticsRest(streamConfig))
    }

    @Test
    fun testGetEventServiceDefaultDestination() {
        val streamConfig = StreamConfig().also {
            it.streamName = "test"
        }
        assertNotNull(getAnalyticsRest(streamConfig))
    }

    @Test
    fun testStreamConfigMapSerializationDeserialization() {
        val originalStreamConfigs = JsonUtil.decodeFromString<StreamConfigCollection>(TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE))!!.streamConfigs
        Prefs.streamConfigs = originalStreamConfigs
        val restoredStreamConfigs = Prefs.streamConfigs
        assertEquals(JsonUtil.encodeToString(originalStreamConfigs), JsonUtil.encodeToString(restoredStreamConfigs))
    }

    companion object {
        private const val STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json"
    }
}
