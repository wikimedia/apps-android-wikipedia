package org.wikimedia.testkitchen.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikimedia.testkitchen.config.sampling.SampleConfig

@Serializable
class StreamConfig {
    @SerialName("stream") var streamName: String = ""
    @SerialName("canary_events_enabled") var canaryEventsEnabled = false
    @SerialName("destination_event_service") val destinationEventServiceKey: String = "eventgate-analytics-external"
    @SerialName("schema_title") var schemaTitle: String? = null
    @SerialName("producers") var producerConfig: ProducerConfig? = null
    @SerialName("sample") var sampleConfig: SampleConfig? = null
    @SerialName("topic_prefixes") val topicPrefixes: List<String> = emptyList()

    @Transient
    var destinationEventService: DestinationEventService = DestinationEventService.ANALYTICS

    val topics: List<String> = emptyList()

    fun hasRequestedContextValuesConfig(): Boolean {
        return producerConfig?.metricsPlatformClientConfig?.requestedValues != null
    }

    val events
        get() = producerConfig?.metricsPlatformClientConfig?.events.orEmpty()

    fun hasCurationFilter(): Boolean {
        return producerConfig?.metricsPlatformClientConfig?.curationFilter != null
    }

    val curationFilter get() = producerConfig?.metricsPlatformClientConfig?.curationFilter ?: CurationFilter()

    init {
        destinationEventService = DestinationEventService.entries.find { it.id == destinationEventServiceKey } ?: DestinationEventService.ANALYTICS
    }

    @Serializable
    class ProducerConfig {
        @SerialName("metrics_platform_client") var metricsPlatformClientConfig: MetricsPlatformClientConfig? = null
    }

    @Serializable
    class MetricsPlatformClientConfig {
        // TODO: how is this used?
        var events: List<String>? = null
        @SerialName("provide_values") var requestedValues: List<String>? = null
        @SerialName("curation") var curationFilter: CurationFilter? = null
    }

    // TODO: how is this used?
    fun isInterestedInEvent(eventName: String): Boolean {
        for (streamEventName in this.events) {
            // Match string prefixes for event names of interested streams.
            if (eventName.startsWith(streamEventName)) {
                return true
            }
        }
        return false
    }
}
