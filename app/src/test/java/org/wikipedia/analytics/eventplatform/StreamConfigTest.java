package org.wikipedia.analytics.eventplatform;

import com.google.gson.Gson;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse;
import org.wikipedia.test.TestFileUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.wikipedia.analytics.eventplatform.DestinationEventService.ANALYTICS;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.json.GsonUtil.getDefaultGson;

@SuppressWarnings("checkstyle:magicnumber")
public class StreamConfigTest {

    private static final String STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json";
    private static final Gson GSON = getDefaultGson();

    @Test
    public void testStreamConfigResponseDeserialization() throws IOException {
        String json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE);
        MwStreamConfigsResponse response = GSON.fromJson(json, MwStreamConfigsResponse.class);
        Map<String, StreamConfig> streamConfigs = response.getStreamConfigs();

        assertThat(streamConfigs.containsKey("test.event"), is(true));
        StreamConfig streamConfig = streamConfigs.get("test.event");

        assertThat(streamConfig.getStreamName(), is("test.event"));
        assertThat(streamConfig.getSchemaTitle(), is("test/event"));
        assertThat(streamConfig.areCanaryEventsEnabled(), is(true));
        assertThat(streamConfig.getDestinationEventService(), is(ANALYTICS));
        assertThat(streamConfig.getTopicPrefixes(), is(Arrays.asList("eqiad.", "codfw.")));
        assertThat(streamConfig.getTopics(), is(Arrays.asList("eqiad.test.event", "codfw.test.event")));

        SamplingConfig samplingConfig = streamConfig.getSamplingConfig();
        assertThat(samplingConfig.getIdentifier(), is(DEVICE));
        assertThat(samplingConfig.getRate(), is(0.5));
    }

}
