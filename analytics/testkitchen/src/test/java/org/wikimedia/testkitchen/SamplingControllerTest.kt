package org.wikimedia.testkitchen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikimedia.testkitchen.context.DataFixtures

internal class SamplingControllerTest {
    private val samplingController = SamplingController(
        DataFixtures.getTestClientData(),
        SessionController()
    )

    @Test
    fun testGetSamplingValue() {
        val deviceVal = samplingController.getSamplingValue(SampleConfig.UNIT_DEVICE)
        assertTrue(deviceVal >= 0.0)
        assertTrue(deviceVal <= 1.0)
    }

    @Test
    fun testGetSamplingId() {
        assertNotNull(samplingController.getSamplingId(SampleConfig.UNIT_DEVICE))
        assertNotNull(samplingController.getSamplingId(SampleConfig.UNIT_SESSION))
    }

    @Test
    fun testNoSamplingConfig() {
        val noSamplingConfig = StreamConfig().also {
            it.streamName = "foo"
            it.schemaTitle = "bar"
        }
        assertTrue(samplingController.isInSample(noSamplingConfig))
    }

    @Test
    fun testAlwaysInSample() {
        val alwaysInSample = StreamConfig().also {
            it.streamName = "foo"
            it.schemaTitle = "bar"
            it.producerConfig = StreamConfig.ProducerConfig().also { pc ->
                pc.metricsPlatformClientConfig = StreamConfig.MetricsPlatformClientConfig()
            }
        }
        assertTrue(samplingController.isInSample(alwaysInSample))
    }

    @Test
    fun testNeverInSample() {
        val neverInSample = StreamConfig().also {
            it.streamName = "foo"
            it.schemaTitle = "bar"
            it.producerConfig = StreamConfig.ProducerConfig().also { pc ->
                pc.metricsPlatformClientConfig = StreamConfig.MetricsPlatformClientConfig()
            }
            it.sampleConfig = SampleConfig(0.0, SampleConfig.UNIT_SESSION)
        }
        assertFalse(samplingController.isInSample(neverInSample))
    }
}
