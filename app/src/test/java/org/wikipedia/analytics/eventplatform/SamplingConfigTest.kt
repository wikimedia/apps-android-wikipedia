package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.wikipedia.json.GsonUtil

class SamplingConfigTest {
    @Test
    fun testSamplingConfigDeserializationDefaults() {
        val json = "{}"
        assertDeserializedValues(json, 1.0, SamplingConfig.Identifier.SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultIdentifierOnly() {
        val json = "{\"rate\": 0.5}"
        assertDeserializedValues(json, 0.5, SamplingConfig.Identifier.SESSION)
    }

    @Test
    fun testSamplingConfigDeserializationDefaultRateOnly() {
        val json = "{\"identifier\": \"device\"}"
        assertDeserializedValues(json, 1.0, SamplingConfig.Identifier.DEVICE)
    }

    @Test
    fun testSamplingConfigDeserializationNoDefaults() {
        val json = "{\"rate\": 0.325, \"identifier\": \"pageview\"}"
        assertDeserializedValues(json, 0.325, SamplingConfig.Identifier.PAGEVIEW)
    }

    private fun assertDeserializedValues(json: String, rate: Double, identifier: SamplingConfig.Identifier) {
        val config = GSON.fromJson(json, SamplingConfig::class.java)
        MatcherAssert.assertThat(config.rate, CoreMatchers.equalTo(rate))
        MatcherAssert.assertThat(config.getIdentifier(), CoreMatchers.equalTo(identifier))
    }

    companion object {
        private val GSON = GsonUtil.getDefaultGson()
    }
}
