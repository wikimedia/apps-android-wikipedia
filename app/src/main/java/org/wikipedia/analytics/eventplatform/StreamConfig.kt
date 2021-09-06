package org.wikipedia.analytics.eventplatform

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.DestinationEventService.ANALYTICS

@Serializable
class StreamConfig {

    @SerialName("stream") @SerializedName("stream")
    var streamName = ""

    @SerialName("canary_events_enabled") @SerializedName("canary_events_enabled")
    var canaryEventsEnabled = false

    @SerialName("destination_event_service") @SerializedName("destination_event_service")
    var destinationEventService: DestinationEventService = ANALYTICS

    @SerialName("schema_title") @SerializedName("schema_title")
    val schemaTitle: String = ""

    @SerialName("topic_prefixes") @SerializedName("topic_prefixes")
    val topicPrefixes: List<String> = emptyList()
    val topics: List<String> = emptyList()

    @SerialName("sampling") @SerializedName("sampling")
    val samplingConfig: SamplingConfig? = null
}
