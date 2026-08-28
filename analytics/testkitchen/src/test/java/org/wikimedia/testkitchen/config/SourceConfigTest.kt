package org.wikimedia.testkitchen.config

import org.junit.Assert.assertTrue
import org.junit.Test

internal class SourceConfigTest {
    @Test
    fun testGetStreamNames() {
        val sourceConfig = SourceConfig(StreamConfigFixtures.STREAM_CONFIGS_WITH_EVENTS)
        assertTrue(sourceConfig.streamConfigs.keys.size == 1 && sourceConfig.streamConfigs.keys.contains("test.stream"))
    }
}
