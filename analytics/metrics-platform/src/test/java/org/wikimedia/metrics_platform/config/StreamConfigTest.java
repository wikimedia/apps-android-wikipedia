package org.wikimedia.metrics_platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metrics_platform.config.sampling.SampleConfig.Identifier.SESSION;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;

class StreamConfigTest {

    @Test void testStreamConfigDeserialization() {
        Gson gson = GsonHelper.getGson();
        String streamConfigJson = "{\"stream\":\"test.event\",\"schema_title\":\"test/event\"," +
                "\"destination_event_service\":\"eventgate-logging-local\"," +
                "\"producers\":" +
                "{\"metrics_platform_client\":{\"provide_values\":[\"page_id\",\"user_id\"]}}," +
                "\"sample\":{\"rate\":0.5,\"identifier\":\"session\"}}";
        StreamConfig streamConfig = gson.fromJson(streamConfigJson, StreamConfig.class);
        assertThat(streamConfig.getStreamName()).isEqualTo("test.event");
        assertThat(streamConfig.getSchemaTitle()).isEqualTo("test/event");
        assertThat(streamConfig.getSampleConfig().getRate()).isEqualTo(0.5);
        assertThat(streamConfig.getSampleConfig().getIdentifier()).isEqualTo(SESSION);
        assertThat(streamConfig.getProducerConfig().getMetricsPlatformClientConfig().getRequestedValues())
                .isEqualTo(Set.of("page_id", "user_id"));
        assertThat(streamConfig.getDestinationEventService()).isEqualTo(DestinationEventService.LOCAL);
    }
}
