package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.wikipedia.json.JsonUtil

class SamplingConfigTest {
    @Test
    fun testSamplingConfigDeserializationDefaults() {
        val json = "{}"
        assertDeserializedValues(json, 1.0, SamplingConfig.UNIT_SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultIdentifierOnly() {
        val json = "{\"rate\": 0.5}"
        assertDeserializedValues(json, 0.5, SamplingConfig.UNIT_SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultRateOnly() {
        val json = "{\"unit\": \"device\"}"
        assertDeserializedValues(json, 1.0, SamplingConfig.UNIT_DEVICE)
    }

    @Test
    fun testSamplingConfigDeserializationNoDefaults() {
        val json = "{\"rate\": 0.325, \"unit\": \"pageview\"}"
        assertDeserializedValues(json, 0.325, SamplingConfig.UNIT_PAGEVIEW)
    }

    private fun assertDeserializedValues(json: String, rate: Double, unit: String) {
        val config = JsonUtil.decodeFromString<SamplingConfig>(json)!!
        MatcherAssert.assertThat(config.rate, CoreMatchers.equalTo(rate))
        MatcherAssert.assertThat(config.unit, CoreMatchers.equalTo(unit))
    }
}
