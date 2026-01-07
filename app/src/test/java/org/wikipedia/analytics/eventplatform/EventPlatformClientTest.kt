package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Ignore
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
        MatcherAssert.assertThat(id.length, CoreMatchers.`is`(20))
    }

    @Test
    fun testGetStream() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "test"
        })
        MatcherAssert.assertThat(
            EventPlatformClient.getStreamConfig("test"), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            EventPlatformClient.getStreamConfig("key.does.not.exist"), CoreMatchers.`is`(CoreMatchers.nullValue())
        )
    }

    @Test
    fun testEventSerialization() {
        val event = TestAppsEvent("test")
        val serialized = JsonUtil.encodeToString(event)!!
        MatcherAssert.assertThat(serialized.contains("dt"), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(serialized.contains("app_session_id"), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(serialized.contains("app_install_id"), CoreMatchers.`is`(true))
    }

    @Test
    fun testAssociationControllerGetPageViewId() {
        val pageViewId = EventPlatformClient.AssociationController.pageViewId
        MatcherAssert.assertThat(pageViewId.length, CoreMatchers.equalTo(20))
    }

    @Test
    fun testAssociationControllerGetSessionId() {
        val sessionId = EventPlatformClient.AssociationController.sessionId
        MatcherAssert.assertThat(sessionId.length, CoreMatchers.equalTo(20))
        val persistentSessionId = Prefs.eventPlatformSessionId
        MatcherAssert.assertThat(persistentSessionId, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        MatcherAssert.assertThat(persistentSessionId!!.length, CoreMatchers.equalTo(20))
    }

    @Test
    fun testNeverInSampleIfNoStreamConfig() {
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("not-configured")),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun testAlwaysInSampleIfStreamConfiguredButNoSamplingConfig() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "configured"
        })
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("configured")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testAlwaysInSample() {
        EventPlatformClient.setStreamConfig(StreamConfig().also {
            it.streamName = "alwaysInSample"
            it.sampleConfig = SampleConfig(1.0)
        })
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("alwaysInSample")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testNeverInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig().also {
                it.streamName = "neverInSample"
                it.sampleConfig = SampleConfig(0.0)
            }
        )
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("neverInSample")),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun testSamplingControllerGetSamplingValue() {
        val deviceVal = SamplingController.getSamplingValue(SampleConfig.UNIT_DEVICE)
        MatcherAssert.assertThat(deviceVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(deviceVal, Matchers.lessThanOrEqualTo(1.0))
        val pageViewVal = SamplingController.getSamplingValue(SampleConfig.UNIT_PAGEVIEW)
        MatcherAssert.assertThat(pageViewVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(pageViewVal, Matchers.lessThanOrEqualTo(1.0))
        val sessionVal = SamplingController.getSamplingValue(SampleConfig.UNIT_SESSION)
        MatcherAssert.assertThat(sessionVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(sessionVal, Matchers.lessThanOrEqualTo(1.0))
    }

    @Test
    fun testSamplingControllerGetSamplingId() {
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SampleConfig.UNIT_DEVICE), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SampleConfig.UNIT_PAGEVIEW), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SampleConfig.UNIT_SESSION), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Test
    fun testGetEventService() {
        val streamConfig = StreamConfig().also {
            it.streamName = "test"
            it.destinationEventService = DestinationEventService.LOGGING
        }
        MatcherAssert.assertThat(
            getAnalyticsRest(streamConfig),
            CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Test
    fun testGetEventServiceDefaultDestination() {
        val streamConfig = StreamConfig().also {
            it.streamName = "test"
        }
        MatcherAssert.assertThat(
            getAnalyticsRest(streamConfig),
            CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Ignore("Disabled because of flakiness on CI systems, and only marginally useful.")
    @Test
    fun testStreamConfigMapSerializationDeserialization() {
        val originalStreamConfigs = JsonUtil.decodeFromString<StreamConfigCollection>(TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE))!!.streamConfigs
        Prefs.streamConfigs = originalStreamConfigs
        val restoredStreamConfigs = Prefs.streamConfigs
        MatcherAssert.assertThat(
            JsonUtil.encodeToString(restoredStreamConfigs),
            CoreMatchers.`is`(JsonUtil.encodeToString(originalStreamConfigs))
        )
    }

    companion object {
        private const val STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json"
    }
}
