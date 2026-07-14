package org.wikimedia.testkitchen.config

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikimedia.testkitchen.JsonUtil
import org.wikimedia.testkitchen.config.sampling.SampleConfig

internal class StreamConfigTest {
    @Test
    fun testStreamConfigDeserialization() {
        val streamConfigJson = "{\"stream\":\"test.event\",\"schema_title\":\"test/event\"," +
                "\"destination_event_service\":\"eventgate-logging-local\"," +
                "\"producers\":" +
                "{\"metrics_platform_client\":{\"provide_values\":[\"page_id\",\"user_id\"]}}," +
                "\"sample\":{\"rate\":0.5,\"identifier\":\"session\"}}"
        val streamConfig: StreamConfig = JsonUtil.decodeFromString(streamConfigJson)!!

        assertEquals("test.event", streamConfig.streamName)
        assertEquals("test/event", streamConfig.schemaTitle)
        assertEquals(0.5, streamConfig.sampleConfig!!.rate, 0.0000001)
        assertEquals(SampleConfig.UNIT_SESSION, streamConfig.sampleConfig!!.unit)
        assertEquals(listOf("page_id", "user_id"), streamConfig.producerConfig!!.metricsPlatformClientConfig!!.requestedValues)
        assertEquals(DestinationEventService.LOCAL, streamConfig.destinationEventService)
    }
}
