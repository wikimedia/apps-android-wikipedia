package org.wikipedia.analytics.eventplatform

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName

class StreamConfig {

    @SerializedName("stream")
    var streamName = ""

    @SerializedName("canary_events_enabled")
    var canaryEventsEnabled = false

    @SerializedName("destination_event_service")
    var destinationEventService = DestinationEventService.ANALYTICS

    @SerializedName("schema_title")
    val schemaTitle: String = ""

    @SerializedName("topic_prefixes")
    val topicPrefixes: List<String> = emptyList()
    val topics: List<String> = emptyList()

    @SerializedName("sampling")
    val samplingConfig: SamplingConfig?

    @VisibleForTesting
    constructor(streamName: String, samplingConfig: SamplingConfig?,
                destinationEventService: DestinationEventService) {
        this.streamName = streamName
        this.samplingConfig = samplingConfig
        this.destinationEventService = destinationEventService
    }
}
