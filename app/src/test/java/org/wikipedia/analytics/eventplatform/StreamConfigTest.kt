package org.wikipedia.analytics.eventplatform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.config.StreamConfigCollection
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil
import java.io.IOException

class StreamConfigTest {
    @Test
    @Throws(IOException::class)
    fun testStreamConfigResponseDeserialization() {
        val json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE)
        val response = JsonUtil.decodeFromString<StreamConfigCollection>(json)!!
        val streamConfigs = response.streamConfigs
        assertTrue(streamConfigs.containsKey("test.event"))
        val streamConfig = streamConfigs["test.event"]
        assertNotNull(streamConfig)
        assertEquals("test.event", streamConfig!!.streamName)
        assertEquals("test/event", streamConfig.schemaTitle)
        assertEquals(true, streamConfig.canaryEventsEnabled)
        assertEquals(DestinationEventService.ANALYTICS, streamConfig.destinationEventService)
        assertEquals(listOf("eqiad.", "codfw."), streamConfig.topicPrefixes)
        assertEquals(listOf("eqiad.test.event", "codfw.test.event"), streamConfig.topics)
        val samplingConfig = streamConfig.sampleConfig
        assertNotNull(samplingConfig)
        assertEquals(SampleConfig.UNIT_DEVICE, samplingConfig!!.unit)
        assertEquals(0.5, samplingConfig.rate, 0.0)
    }

    companion object {
        private const val STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json"
    }
}
