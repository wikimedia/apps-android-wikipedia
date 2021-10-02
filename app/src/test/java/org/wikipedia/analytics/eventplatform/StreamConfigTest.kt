package org.wikipedia.analytics.eventplatform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil
import java.io.IOException

class StreamConfigTest {
    @Test
    @Throws(IOException::class)
    fun testStreamConfigResponseDeserialization() {
        val json = TestFileUtil.readRawFile(STREAM_CONFIGS_RESPONSE)
        val response = JsonUtil.decodeFromString<MwStreamConfigsResponse>(json)!!
        val streamConfigs = response.streamConfigs
        MatcherAssert.assertThat(streamConfigs.containsKey("test.event"), CoreMatchers.`is`(true))
        val streamConfig = streamConfigs["test.event"]
        MatcherAssert.assertThat(streamConfig!!.streamName, CoreMatchers.`is`("test.event"))
        MatcherAssert.assertThat(streamConfig.schemaTitle, CoreMatchers.`is`("test/event"))
        MatcherAssert.assertThat(streamConfig.canaryEventsEnabled, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(streamConfig.destinationEventService, CoreMatchers.`is`(DestinationEventService.ANALYTICS))
        MatcherAssert.assertThat(streamConfig.topicPrefixes, CoreMatchers.`is`(listOf("eqiad.", "codfw.")))
        MatcherAssert.assertThat(streamConfig.topics, CoreMatchers.`is`(listOf("eqiad.test.event", "codfw.test.event")))
        val samplingConfig = streamConfig.samplingConfig
        MatcherAssert.assertThat(samplingConfig!!.getIdentifier(), CoreMatchers.`is`(SamplingConfig.Identifier.DEVICE))
        MatcherAssert.assertThat(samplingConfig.rate, CoreMatchers.`is`(0.5))
    }

    companion object {
        private const val STREAM_CONFIGS_RESPONSE = "streamconfigs_response.json"
    }
}
