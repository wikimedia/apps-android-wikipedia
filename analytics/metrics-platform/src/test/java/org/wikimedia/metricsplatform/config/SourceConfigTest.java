package org.wikimedia.metricsplatform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceConfigTest {

    private static SourceConfig sourceConfig;

    @Test void testConfig() {
        sourceConfig = new SourceConfig(StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS);
        assertThat(sourceConfig.getStreamNamesByEvent("test.event")).containsExactly("test.stream");
    }

    @Test void testGetStreamNames() {
        sourceConfig = new SourceConfig(StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS);
        assertThat(sourceConfig.getStreamNames()).containsExactly("test.stream");
    }

    @Test void testGetStreamConfigByName() {
        sourceConfig = new SourceConfig(StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS);
        StreamConfig streamConfig = StreamConfigFixtures.sampleStreamConfig(true);
        assertThat(streamConfig).isEqualTo(sourceConfig.getStreamConfigByName("test.stream"));
    }

    @Test void testGetStreamNamesByEvent() {
        sourceConfig = new SourceConfig(StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS);
        assertThat(sourceConfig.getStreamNamesByEvent("test.event")).containsExactly("test.stream");
    }
}
