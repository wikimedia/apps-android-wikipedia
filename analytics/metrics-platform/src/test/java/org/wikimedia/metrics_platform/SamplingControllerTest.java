package org.wikimedia.metrics_platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metrics_platform.config.sampling.SampleConfig.Identifier.DEVICE;
import static org.wikimedia.metrics_platform.config.sampling.SampleConfig.Identifier.SESSION;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.config.sampling.SampleConfig;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.context.DataFixtures;

class SamplingControllerTest {

    private final SamplingController samplingController = new SamplingController(
            DataFixtures.getTestClientData(),
            new SessionController()
    );

    @Test void testGetSamplingValue() {
        double deviceVal = samplingController.getSamplingValue(DEVICE);
        assertThat(deviceVal).isBetween(0.0, 1.0);
    }

    @Test void testGetSamplingId() {
        assertThat(samplingController.getSamplingId(DEVICE)).isNotNull();
        assertThat(samplingController.getSamplingId(SESSION)).isNotNull();
    }

    @Test void testNoSamplingConfig() {
        StreamConfig noSamplingConfig = new StreamConfig("foo", "bar", null, null, null);
        assertThat(samplingController.isInSample(noSamplingConfig)).isTrue();
    }

    @Test void testAlwaysInSample() {
        StreamConfig alwaysInSample = new StreamConfig("foo", "bar", null,
                new StreamConfig.ProducerConfig(new StreamConfig.MetricsPlatformClientConfig(
                        null,
                        null,
                        null
                )),
                null
        );
        assertThat(samplingController.isInSample(alwaysInSample)).isTrue();
    }

    @Test void testNeverInSample() {
        StreamConfig neverInSample = new StreamConfig("foo", "bar", null,
                new StreamConfig.ProducerConfig(new StreamConfig.MetricsPlatformClientConfig(
                        null,
                        null,
                        null
                )),
                new SampleConfig(0.0, SESSION)
        );
        assertThat(samplingController.isInSample(neverInSample)).isFalse();
    }

}
