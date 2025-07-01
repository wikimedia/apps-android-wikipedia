package org.wikimedia.metricsplatform.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metricsplatform.config.DestinationEventService.LOCAL;
import static org.wikimedia.metricsplatform.config.StreamConfigFetcher.ANALYTICS_API_ENDPOINT;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.google.common.io.Resources;

import okhttp3.OkHttpClient;

class StreamConfigFetcherTest {

    @Test void parsingConfigFromJsonWorks() throws IOException {
        try (Reader in = readConfigFile("streamconfigs.json")) {
            StreamConfigFetcher streamConfigFetcher = new StreamConfigFetcher(new URL(ANALYTICS_API_ENDPOINT), new OkHttpClient(), GsonHelper.getGson());
            Map<String, StreamConfig> config = streamConfigFetcher.parseConfig(in);
            assertThat(config).containsKey("mediawiki.visual_editor_feature_use");
            StreamConfig streamConfig = config.get("mediawiki.visual_editor_feature_use");
            String schemaTitle = streamConfig.getSchemaTitle();
            assertThat(schemaTitle).isEqualTo("analytics/mediawiki/client/metrics_event");
            assertThat(streamConfig.getStreamName()).isEqualTo("mediawiki.visual_editor_feature_use");
        }
    }

    @Test void parsingLocalConfigFromJsonWorks() throws IOException {
        try (Reader in = readConfigFile("streamconfigs-local.json")) {
            StreamConfigFetcher streamConfigFetcher = new StreamConfigFetcher(new URL(ANALYTICS_API_ENDPOINT), new OkHttpClient(), GsonHelper.getGson());
            Map<String, StreamConfig> config = streamConfigFetcher.parseConfig(in);
            assertThat(config).containsKey("mediawiki.visual_editor_feature_use");
            StreamConfig streamConfig = config.get("mediawiki.edit_attempt");
            assertThat(streamConfig.getDestinationEventService()).isEqualTo(LOCAL);
        }
    }

    private Reader readConfigFile(String filename) throws IOException {
        return Resources.asCharSource(
                Resources.getResource("org/wikimedia/metrics_platform/config/" + filename),
                UTF_8
        ).openStream();
    }
}
