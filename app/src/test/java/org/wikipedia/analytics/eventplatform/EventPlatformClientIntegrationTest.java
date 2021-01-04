package org.wikipedia.analytics.eventplatform;

import com.google.gson.Gson;

import org.junit.Test;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.wikipedia.analytics.eventplatform.DestinationEventService.LOGGING;
import static org.wikipedia.analytics.eventplatform.EventPlatformClient.setStreamConfig;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getIso8601Timestamp;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getStoredStreamConfigs;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.postEvents;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.setStoredStreamConfigs;
import static org.wikipedia.json.GsonUtil.getDefaultGson;

@SuppressWarnings("checkstyle:magicnumber")
public class EventPlatformClientIntegrationTest extends MockRetrofitTest {

    private static final String STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json";
    private static final Gson GSON = getDefaultGson();

    @Test
    public void testGetEventService() {
        StreamConfig streamConfig = new StreamConfig("test", null, LOGGING);
        assertThat(ServiceFactory.getAnalyticsRest(streamConfig), is(notNullValue()));
    }

    @Test
    public void testGetEventServiceDefaultDestination() {
        StreamConfig streamConfig = new StreamConfig("test");
        assertThat(ServiceFactory.getAnalyticsRest(streamConfig), is(notNullValue()));
    }

    @Test
    public void testGetIso8601Timestamp() {
        String deviceId = getIso8601Timestamp();
        assertThat(deviceId, matchesPattern("^202\\d-[0-1]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\dZ$"));
    }

    @Test
    public void testPostEventsSuccess() {
        List<Event> events = new ArrayList<>();
        StreamConfig streamConfig = new StreamConfig("test");
        setStreamConfig(streamConfig);
        server().enqueue(new MockResponse().setResponseCode(202));
        events.add(new Event("test", "test"));
        postEvents(streamConfig, events);
    }

    @Test
    public void testDoesNotThrowOnPostEventsFailureResponse() {
        List<Event> events = new ArrayList<>();
        StreamConfig streamConfig = new StreamConfig("test");
        setStreamConfig(streamConfig);
        server().enqueue(new MockResponse().setResponseCode(400));
        events.add(new Event("test", "test"));
        postEvents(streamConfig, events);
    }

    @Test
    public void testStreamConfigMapSerializationDeserialization() throws IOException {
        String json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE);
        MwStreamConfigsResponse response = GSON.fromJson(json, MwStreamConfigsResponse.class);
        Map<String, StreamConfig> originalStreamConfigs = response.getStreamConfigs();

        setStoredStreamConfigs(originalStreamConfigs);
        Map<String, StreamConfig> restoredStreamConfigs = getStoredStreamConfigs();
        assertThat(GsonMarshaller.marshal(restoredStreamConfigs), is(GsonMarshaller.marshal(originalStreamConfigs)));
    }

}
