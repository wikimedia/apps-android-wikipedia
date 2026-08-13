package org.wikipedia.analytics.eventplatform

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikipedia.json.JsonUtil

class SamplingConfigTest {
    @Test
    fun testSamplingConfigDeserializationDefaults() {
        val json = "{}"
        assertDeserializedValues(json, 1.0, SampleConfig.UNIT_SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultIdentifierOnly() {
        val json = "{\"rate\": 0.5}"
        assertDeserializedValues(json, 0.5, SampleConfig.UNIT_SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultRateOnly() {
        val json = "{\"unit\": \"device\"}"
        assertDeserializedValues(json, 1.0, SampleConfig.UNIT_DEVICE)
    }

    @Test
    fun testSamplingConfigDeserializationNoDefaults() {
        val json = "{\"rate\": 0.325, \"unit\": \"pageview\"}"
        assertDeserializedValues(json, 0.325, SampleConfig.UNIT_PAGEVIEW)
    }

    private fun assertDeserializedValues(json: String, rate: Double, unit: String) {
        val config = JsonUtil.decodeFromString<SampleConfig>(json)!!
        assertEquals(rate, config.rate, 0.0)
        assertEquals(unit, config.unit)
    }
}
