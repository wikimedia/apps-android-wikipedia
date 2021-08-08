package org.wikipedia.analytics.eventplatform

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class StreamConfig(
    @Json(name = "stream") val streamName: String = "",
    @Json(name = "schema_title") val schemaTitle: String = "",
    @Json(name = "topic_prefixes") val topicPrefixes: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    @Json(name = "canary_events_enabled") var canaryEventsEnabled: Boolean = false,
    @Json(name = "destination_event_service") val destinationEventService: DestinationEventService = DestinationEventService.ANALYTICS,
    @Json(name = "sampling") val samplingConfig: SamplingConfig? = null
) {
    /**
     * Constructor for testing.
     *
     * In practice, field values will be set by Gson during deserialization using reflection.
     *
     * @param streamName stream name
     * @param destinationEventService destination
     */
    @VisibleForTesting
    internal constructor(streamName: String, samplingConfig: SamplingConfig?, destinationEventService: DestinationEventService) :
            this(streamName, schemaTitle = "", destinationEventService = destinationEventService, samplingConfig = samplingConfig)
}
