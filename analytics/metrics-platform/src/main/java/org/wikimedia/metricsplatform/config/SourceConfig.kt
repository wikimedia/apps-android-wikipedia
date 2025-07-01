package org.wikimedia.metricsplatform.config

class SourceConfig(streamConfigs: Map<String, StreamConfig>) {
    /**
     * Get all stream configs with stream name.
     */
    val streamConfigsMap = streamConfigs

    /**
     * Get all stream configs without stream name.
     */
    val streamConfigsRaw = streamConfigs.values

    val streamNames get() = streamConfigsMap.keys

    /**
     * Get stream config by stream name.
     *
     * @param streamName stream name
     */
    fun getStreamConfigByName(streamName: String?): StreamConfig? {
        return streamConfigsMap[streamName]
    }

    /**
     * Get stream names by event name.
     *
     * @param eventName event name
     */
    fun getStreamNamesByEvent(eventName: String): Collection<String> {
        return streamConfigsRaw
            .filter { streamConfig: StreamConfig -> streamConfig.isInterestedInEvent(eventName) }
            .map { streamConfig: StreamConfig -> streamConfig.streamName }
    }
}
