package org.wikipedia.analytics.eventplatform

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StreamConfig {

    @SerialName("stream")
    var streamName = ""

    @SerialName("canary_events_enabled")
    var canaryEventsEnabled = false

    @SerialName("destination_event_service")
    private var destinationEventService: DestinationEventService?

    @SerialName("schema_title")
    val schemaTitle: String = ""

    @SerialName("topic_prefixes")
    val topicPrefixes: List<String> = emptyList()
    val topics: List<String> = emptyList()

    @SerialName("sampling")
    val samplingConfig: SamplingConfig?

    fun getDestinationEventService(): DestinationEventService {
        return destinationEventService ?: DestinationEventService.ANALYTICS
    }

    @VisibleForTesting
    constructor(streamName: String, samplingConfig: SamplingConfig?, destinationEventService: DestinationEventService?) {
        this.streamName = streamName
        this.samplingConfig = samplingConfig
        this.destinationEventService = destinationEventService
    }
}
