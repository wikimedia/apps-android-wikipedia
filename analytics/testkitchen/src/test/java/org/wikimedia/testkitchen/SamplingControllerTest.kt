package org.wikimedia.testkitchen

import org.assertj.core.api.Assertions.assertThat

internal class SamplingControllerTest {
    private val samplingController = SamplingController(
        DataFixtures.getTestClientData(),
        SessionController()
    )

    @Test
    fun testGetSamplingValue() {
        val deviceVal = samplingController.getSamplingValue(DEVICE)
        assertThat(deviceVal).isBetween(0.0, 1.0)
    }

    @Test
    fun testGetSamplingId() {
        assertThat(samplingController.getSamplingId(DEVICE)).isNotNull()
        assertThat(samplingController.getSamplingId(SESSION)).isNotNull()
    }

    @Test
    fun testNoSamplingConfig() {
        val noSamplingConfig: StreamConfig = StreamConfig("foo", "bar", null, null, null)
        assertThat(samplingController.isInSample(noSamplingConfig)).isTrue()
    }

    @Test
    fun testAlwaysInSample() {
        val alwaysInSample: StreamConfig = StreamConfig(
            "foo", "bar", null,
            ProducerConfig(
                MetricsPlatformClientConfig(
                    null,
                    null,
                    null
                )
            ),
            null
        )
        assertThat(samplingController.isInSample(alwaysInSample)).isTrue()
    }

    @Test
    fun testNeverInSample() {
        val neverInSample: StreamConfig = StreamConfig(
            "foo", "bar", null,
            ProducerConfig(
                MetricsPlatformClientConfig(
                    null,
                    null,
                    null
                )
            ),
            SampleConfig(0.0, SESSION)
        )
        assertThat(samplingController.isInSample(neverInSample)).isFalse()
    }
}
