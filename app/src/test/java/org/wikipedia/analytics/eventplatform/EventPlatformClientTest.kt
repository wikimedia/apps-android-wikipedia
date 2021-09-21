package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient.SamplingController
import org.wikipedia.dataclient.ServiceFactory.getAnalyticsRest
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.settings.Prefs
import org.wikipedia.test.TestFileUtil
import java.io.IOException
import java.util.*

@RunWith(RobolectricTestRunner::class)
class EventPlatformClientTest {
    @Before
    fun reset() {
        EventPlatformClient.STREAM_CONFIGS = HashMap()
        EventPlatformClient.AssociationController.beginNewSession()

        // Set app install ID
        WikipediaApp.getInstance().appInstallID
    }

    @Test
    fun testGenerateRandomId() {
        val id = EventPlatformClient.AssociationController.generateRandomId()
        MatcherAssert.assertThat(id.length, CoreMatchers.`is`(20))
    }

    @Test
    fun testGetStream() {
        EventPlatformClient.setStreamConfig(StreamConfig("test", null, null))
        MatcherAssert.assertThat(
            EventPlatformClient.STREAM_CONFIGS["test"], CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            EventPlatformClient.STREAM_CONFIGS["key.does.not.exist"], CoreMatchers.`is`(CoreMatchers.nullValue())
        )
    }

    @Test
    fun testEventSerialization() {
        val event = Event("test", "test")
        EventPlatformClient.addEventMetadata(event)
        val serialized = GsonMarshaller.marshal(event)
        MatcherAssert.assertThat(serialized.contains("dt"), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(serialized.contains("app_session_id"), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(serialized.contains("app_install_id"), CoreMatchers.`is`(true))
    }

    @Ignore("Disabled for testing: https://phabricator.wikimedia.org/T281001")
    @Test
    fun testOutputBufferEnqueuesEventOnSubmit() {
        val event = Event("test", "test")
        Mockito.mockStatic(EventPlatformClient.OutputBuffer::class.java).use { outputBuffer ->
            Mockito.mockStatic(
                SamplingController::class.java
            ).use { samplingController ->
                samplingController.`when`<Any> { SamplingController.isInSample(event) }
                    .thenReturn(true)
                EventPlatformClient.submit(event)
                outputBuffer.verify(
                    Mockito.times(1),
                    { EventPlatformClient.OutputBuffer.schedule(event) })
            }
        }
    }

    @Test
    fun testOutputBufferSendsEnqueuedEventsOnEnabled() {
        Mockito.mockStatic(EventPlatformClient.OutputBuffer::class.java).use { outputBuffer ->
            EventPlatformClient.setEnabled(true)
            outputBuffer.verify(
                Mockito.times(1),
                { EventPlatformClient.OutputBuffer.sendAllScheduled() })
        }
    }

    @Test
    fun testAssociationControllerGetPageViewId() {
        val pageViewId = EventPlatformClient.AssociationController.getPageViewId()
        MatcherAssert.assertThat(pageViewId.length, CoreMatchers.equalTo(20))
    }

    @Test
    fun testAssociationControllerGetSessionId() {
        val sessionId = EventPlatformClient.AssociationController.getSessionId()
        MatcherAssert.assertThat(sessionId.length, CoreMatchers.equalTo(20))
        val persistentSessionId = Prefs.eventPlatformSessionId
        MatcherAssert.assertThat(persistentSessionId, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        MatcherAssert.assertThat(persistentSessionId!!.length, CoreMatchers.equalTo(20))
    }

    @Test
    fun testNeverInSampleIfNoStreamConfig() {
        MatcherAssert.assertThat(
            SamplingController.isInSample(Event("test", "not-configured")),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun testAlwaysInSampleIfStreamConfiguredButNoSamplingConfig() {
        EventPlatformClient.setStreamConfig(StreamConfig("configured", null, null))
        MatcherAssert.assertThat(
            SamplingController.isInSample(Event("test", "configured")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testAlwaysInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig("alwaysInSample", SamplingConfig(1.0, null), null)
        )
        MatcherAssert.assertThat(
            SamplingController.isInSample(Event("test", "alwaysInSample")),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun testNeverInSample() {
        EventPlatformClient.setStreamConfig(
            StreamConfig("neverInSample", SamplingConfig(0.0, null), null)
        )
        MatcherAssert.assertThat(
            SamplingController.isInSample(Event("test", "neverInSample")),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun testSamplingControllerGetSamplingValue() {
        val deviceVal = SamplingController.getSamplingValue(SamplingConfig.Identifier.DEVICE)
        MatcherAssert.assertThat(deviceVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(deviceVal, Matchers.lessThanOrEqualTo(1.0))
        val pageViewVal = SamplingController.getSamplingValue(SamplingConfig.Identifier.PAGEVIEW)
        MatcherAssert.assertThat(pageViewVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(pageViewVal, Matchers.lessThanOrEqualTo(1.0))
        val sessionVal = SamplingController.getSamplingValue(SamplingConfig.Identifier.SESSION)
        MatcherAssert.assertThat(sessionVal, Matchers.greaterThanOrEqualTo(0.0))
        MatcherAssert.assertThat(sessionVal, Matchers.lessThanOrEqualTo(1.0))
    }

    @Test
    fun testSamplingControllerGetSamplingId() {
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.Identifier.DEVICE), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.Identifier.PAGEVIEW), CoreMatchers.`is`(CoreMatchers.notNullValue())
        )
        MatcherAssert.assertThat(
            SamplingController.getSamplingId(SamplingConfig.Identifier.SESSION), CoreMatchers.`is`(CoreMatchers.notNullValue())
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

    @Test
    @Throws(IOException::class)
    fun testStreamConfigMapSerializationDeserialization() {
        val originalStreamConfigs: Map<String, StreamConfig> = GsonUnmarshaller.unmarshal(
            MwStreamConfigsResponse::class.java,
            TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE)
        ).streamConfigs
        Prefs.setStreamConfigs(originalStreamConfigs)
        val restoredStreamConfigs = Prefs.streamConfigs
        MatcherAssert.assertThat(
            GsonMarshaller.marshal(restoredStreamConfigs),
            CoreMatchers.`is`(GsonMarshaller.marshal(originalStreamConfigs))
        )
    }

    companion object {
        private const val STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json"
    }
}
