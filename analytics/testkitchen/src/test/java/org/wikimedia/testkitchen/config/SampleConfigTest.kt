package org.wikimedia.testkitchen.config

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikimedia.testkitchen.JsonUtil
import org.wikimedia.testkitchen.config.sampling.SampleConfig

internal class SampleConfigTest {
    @Test
    fun testSamplingConfigDeserialization() {
        val samplingConfigJson = "{\"rate\":0.25,\"unit\":\"device\"}"
        val sampleConfig = JsonUtil.decodeFromString<SampleConfig>(samplingConfigJson)!!
        assertEquals(0.25, sampleConfig.rate, 0.0000001)
        assertEquals(SampleConfig.UNIT_DEVICE, sampleConfig.unit)
    }
}
