package org.wikimedia.metricsplatform.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikimedia.metricsplatform.config.sampling.SampleConfig

@Serializable
class StreamConfig {
    @SerialName("stream") var streamName: String = ""
    @SerialName("schema_title") var schemaTitle: String? = null
    @SerialName("destination_event_service") var destinationEventService: DestinationEventService? = null
    @SerialName("producers") var producerConfig: ProducerConfig? = null
    @SerialName("sample") var sampleConfig: SampleConfig? = null

    fun hasRequestedContextValuesConfig(): Boolean {
        return producerConfig?.metricsPlatformClientConfig?.requestedValues != null
    }

    fun hasSampleConfig(): Boolean {
        return producerConfig?.metricsPlatformClientConfig != null && sampleConfig != null
    }

    val events
        get() = producerConfig?.metricsPlatformClientConfig?.events.orEmpty()

    fun hasCurationFilter(): Boolean {
        return producerConfig?.metricsPlatformClientConfig?.curationFilter != null
    }

    val curationFilter get() = producerConfig?.metricsPlatformClientConfig?.curationFilter ?: CurationFilter()

    @Serializable
    class ProducerConfig {
        @SerialName("metrics_platform_client") var metricsPlatformClientConfig: MetricsPlatformClientConfig? = null
    }

    @Serializable
    class MetricsPlatformClientConfig {
        @SerialName("events") var events: List<String>? = null
        @SerialName("provide_values") var requestedValues: List<String>? = null
        @SerialName("curation") var curationFilter: CurationFilter? = null
    }

    fun isInterestedInEvent(eventName: String): Boolean {
        for (streamEventName in this.events) {
            // Match string prefixes for event names of interested streams.
            if (eventName.startsWith(streamEventName)) {
                return true
            }
        }
        return false
    }

    companion object {
        /**
         * The context attributes that the Metrics Platform Client can add to an event.
         */
        val CONTEXTUAL_ATTRIBUTES = arrayOf<String>(
            // Agent
            "agent_app_install_id",
            "agent_client_platform",
            "agent_client_platform_family",  // Page
            "page_id",
            "page_title",
            "page_namespace_id",
            "page_namespace_name",
            "page_revision_id",
            "page_wikidata_qid",
            "page_content_language",  // MediaWiki
            "mediawiki_database",  // Performer
            "performer_is_logged_in",
            "performer_id",
            "performer_name",
            "performer_session_id",
            "performer_pageview_id",
            "performer_groups",
            "performer_language_primary",
            "performer_language_groups",
            "performer_registration_dt",
        )
    }
}
