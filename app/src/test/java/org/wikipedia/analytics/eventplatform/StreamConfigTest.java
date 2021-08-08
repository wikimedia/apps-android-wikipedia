package org.wikipedia.analytics.eventplatform;

import com.squareup.moshi.JsonAdapter;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse;
import org.wikipedia.json.MoshiUtil;
import org.wikipedia.test.TestFileUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.wikipedia.analytics.eventplatform.DestinationEventService.ANALYTICS;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;

@SuppressWarnings("checkstyle:magicnumber")
public class StreamConfigTest {

    private static final String STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json";

    @Test
    public void testStreamConfigResponseDeserialization() throws IOException {
        String json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE);
        final JsonAdapter<MwStreamConfigsResponse> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(MwStreamConfigsResponse.class);
        MwStreamConfigsResponse response = adapter.fromJson(json);
        Map<String, StreamConfig> streamConfigs = response.getStreamConfigs();

        assertThat(streamConfigs.containsKey("test.event"), is(true));
        StreamConfig streamConfig = streamConfigs.get("test.event");

        assertThat(streamConfig.getStreamName(), is("test.event"));
        assertThat(streamConfig.getSchemaTitle(), is("test/event"));
        assertThat(streamConfig.getCanaryEventsEnabled(), is(true));
        assertThat(streamConfig.getDestinationEventService(), is(ANALYTICS));
        assertThat(streamConfig.getTopicPrefixes(), is(Arrays.asList("eqiad.", "codfw.")));
        assertThat(streamConfig.getTopics(), is(Arrays.asList("eqiad.test.event", "codfw.test.event")));

        SamplingConfig samplingConfig = streamConfig.getSamplingConfig();
        assertThat(samplingConfig.getIdentifier(), is(DEVICE));
        assertThat(samplingConfig.getRate(), is(0.5));
    }

}
