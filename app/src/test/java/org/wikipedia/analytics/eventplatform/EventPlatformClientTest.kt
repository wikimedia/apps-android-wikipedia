package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient.SamplingController
import org.wikipedia.dataclient.ServiceFactory.getAnalyticsRest
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse
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
        EventPlatformClient.setStreamConfig(StreamConfig("test", null, null))
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
        EventPlatformClient.setStreamConfig(StreamConfig("configured", null, null))
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("configured")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testAlwaysInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig("alwaysInSample", SamplingConfig(1.0), null)
        )
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("alwaysInSample")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testNeverInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig("neverInSample", SamplingConfig(0.0), null)
        )
        MatcherAssert.assertThat(
            SamplingController.isInSample(TestEvent("neverInSample")),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun testSamplingControllerGetSamplingValue() {
        val deviceVal = SamplingController.getSamplingValue(SamplingConfig.UNIT_DEVICE)
        MatcherAssert.assertThat(deviceVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(deviceVal, Matchers.lessThanOrEqualTo(1.0))
        val pageViewVal = SamplingController.getSamplingValue(SamplingConfig.UNIT_PAGEVIEW)
        MatcherAssert.assertThat(pageViewVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(pageViewVal, Matchers.lessThanOrEqualTo(1.0))
        val sessionVal = SamplingController.getSamplingValue(SamplingConfig.UNIT_SESSION)
        MatcherAssert.assertThat(sessionVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(sessionVal, Matchers.lessThanOrEqualTo(1.0))
    }

    @Test
    fun testSamplingControllerGetSamplingId() {
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.UNIT_DEVICE), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.UNIT_PAGEVIEW), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.UNIT_SESSION), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Test
    fun testGetEventService() {
        val streamConfig = StreamConfig("test", null, DestinationEventService.LOGGING)
        MatcherAssert.assertThat(
            getAnalyticsRest(streamConfig),
            CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Test
    fun testGetEventServiceDefaultDestination() {
        val streamConfig = StreamConfig("test", null, null)
        MatcherAssert.assertThat(
            getAnalyticsRest(streamConfig),
            CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
    }

    @Ignore("Disabled because of flakiness on CI systems, and only marginally useful.")
    @Test
    fun testStreamConfigMapSerializationDeserialization() {
        val originalStreamConfigs = JsonUtil.decodeFromString<MwStreamConfigsResponse>(TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE))!!.streamConfigs
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
