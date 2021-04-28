package org.wikipedia.analytics.eventplatform;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.settings.Prefs;
import org.wikipedia.test.TestFileUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.wikipedia.analytics.eventplatform.DestinationEventService.LOGGING;
import static org.wikipedia.analytics.eventplatform.EventPlatformClient.STREAM_CONFIGS;
import static org.wikipedia.analytics.eventplatform.EventPlatformClient.addEventMetadata;
import static org.wikipedia.analytics.eventplatform.EventPlatformClient.setStreamConfig;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;
import static org.wikipedia.json.GsonUtil.getDefaultGson;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(RobolectricTestRunner.class)
public class EventPlatformClientTest {

    private static final String STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json";
    private static final Gson GSON = getDefaultGson();

    @Before
    public void reset() {
        STREAM_CONFIGS = new HashMap<>();
        EventPlatformClient.AssociationController.beginNewSession();

        // Set app install ID
        WikipediaApp.getInstance().getAppInstallID();
    }

    @Test
    public void testGenerateRandomId() {
        String id = EventPlatformClient.AssociationController.generateRandomId();
        assertThat(id.length(), is(20));
    }

    @Test
    public void testGetStream() {
        setStreamConfig(new StreamConfig("test", null, null));
        assertThat(STREAM_CONFIGS.get("test"), is(notNullValue()));
        assertThat(STREAM_CONFIGS.get("key.does.not.exist"), is(nullValue()));
    }

    @Test
    public void testEventSerialization() {
        Event event = new Event("test", "test");
        addEventMetadata(event);
        String serialized = GsonMarshaller.marshal(event);
        assertThat(serialized.contains("dt"), is(true));
        assertThat(serialized.contains("app_session_id"), is(true));
        assertThat(serialized.contains("app_install_id"), is(true));
    }

    @Test
    public void testOutputBufferEnqueuesEventOnSubmit() {
        Event event = new Event("test", "test");
        try (
                MockedStatic<EventPlatformClient.OutputBuffer> outputBuffer = mockStatic(EventPlatformClient.OutputBuffer.class);
                MockedStatic<EventPlatformClient.SamplingController> samplingController = mockStatic(EventPlatformClient.SamplingController.class)
        ) {
            samplingController.when(() -> EventPlatformClient.SamplingController.isInSample(event)).thenReturn(true);
            EventPlatformClient.submit(event);
            outputBuffer.verify(times(1), () -> EventPlatformClient.OutputBuffer.schedule(event));
        }
    }

    @Test
    public void testOutputBufferSendsEnqueuedEventsOnEnabled() {
        try (MockedStatic<EventPlatformClient.OutputBuffer> outputBuffer = mockStatic(EventPlatformClient.OutputBuffer.class)) {
            EventPlatformClient.setEnabled(true);
            outputBuffer.verify(times(1), EventPlatformClient.OutputBuffer::sendAllScheduled);
        }
    }

    @Test
    public void testAssociationControllerGetPageViewId() {
        String pageViewId = EventPlatformClient.AssociationController.getPageViewId();
        assertThat(pageViewId.length(), equalTo(20));
    }

    @Test
    public void testAssociationControllerGetSessionId() {
        String sessionId = EventPlatformClient.AssociationController.getSessionId();
        assertThat(sessionId.length(), equalTo(20));

        String persistentSessionId = Prefs.getEventPlatformSessionId();
        assertThat(persistentSessionId, is(notNullValue()));
        assertThat(persistentSessionId.length(), equalTo(20));
    }

    @Test
    public void testNeverInSampleIfNoStreamConfig() {
        assertThat(EventPlatformClient.SamplingController.isInSample(new Event("test", "not-configured")), is(false));
    }

    @Test
    public void testAlwaysInSampleIfStreamConfiguredButNoSamplingConfig() {
        setStreamConfig(new StreamConfig("configured", null, null));
        assertThat(EventPlatformClient.SamplingController.isInSample(new Event("test", "configured")), is(true));
    }

    @Test
    public void testAlwaysInSample() {
        setStreamConfig(new StreamConfig("alwaysInSample", new SamplingConfig(1.0, null), null));
        assertThat(EventPlatformClient.SamplingController.isInSample(new Event("test", "alwaysInSample")), is(true));
    }

    @Test
    public void testNeverInSample() {
        setStreamConfig(new StreamConfig("neverInSample", new SamplingConfig(0.0, null), null));
        assertThat(EventPlatformClient.SamplingController.isInSample(new Event("test", "neverInSample")), is(false));
    }

    @Test
    public void testSamplingControllerGetSamplingValue() {
        double deviceVal = EventPlatformClient.SamplingController.getSamplingValue(DEVICE);
        assertThat(deviceVal, greaterThanOrEqualTo(0.0));
        assertThat(deviceVal, lessThanOrEqualTo(1.0));

        double pageViewVal = EventPlatformClient.SamplingController.getSamplingValue(PAGEVIEW);
        assertThat(pageViewVal, greaterThanOrEqualTo(0.0));
        assertThat(pageViewVal, lessThanOrEqualTo(1.0));

        double sessionVal = EventPlatformClient.SamplingController.getSamplingValue(SESSION);
        assertThat(sessionVal, greaterThanOrEqualTo(0.0));
        assertThat(sessionVal, lessThanOrEqualTo(1.0));
    }

    @Test
    public void testSamplingControllerGetSamplingId() {
        assertThat(EventPlatformClient.SamplingController.getSamplingId(DEVICE), is(notNullValue()));
        assertThat(EventPlatformClient.SamplingController.getSamplingId(PAGEVIEW), is(notNullValue()));
        assertThat(EventPlatformClient.SamplingController.getSamplingId(SESSION), is(notNullValue()));
    }

    @Test
    public void testGetEventService() {
        StreamConfig streamConfig = new StreamConfig("test", null, LOGGING);
        assertThat(ServiceFactory.getAnalyticsRest(streamConfig), is(notNullValue()));
    }

    @Test
    public void testGetEventServiceDefaultDestination() {
        StreamConfig streamConfig = new StreamConfig("test", null, null);
        assertThat(ServiceFactory.getAnalyticsRest(streamConfig), is(notNullValue()));
    }

    @Test
    public void testStreamConfigMapSerializationDeserialization() throws IOException {
        String json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE);
        MwStreamConfigsResponse response = GSON.fromJson(json, MwStreamConfigsResponse.class);
        Map<String, StreamConfig> originalStreamConfigs = response.getStreamConfigs();

        Prefs.setStreamConfigs(originalStreamConfigs);
        Map<String, StreamConfig> restoredStreamConfigs = Prefs.getStreamConfigs();
        assertThat(GsonMarshaller.marshal(restoredStreamConfigs), is(GsonMarshaller.marshal(originalStreamConfigs)));
    }

}
