package org.wikimedia.metricsplatform.config

class SourceConfig(val streamConfigs: Map<String, StreamConfig>) {
    fun getStreamConfigByName(streamName: String): StreamConfig? {
        return streamConfigs[streamName]
    }
}
