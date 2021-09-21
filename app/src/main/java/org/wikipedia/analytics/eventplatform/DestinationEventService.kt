package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import org.wikipedia.BuildConfig

/**
 * Possible event destination endpoints which can be specified in stream configurations.
 * https://wikitech.wikimedia.org/wiki/Event_Platform/EventGate#EventGate_clusters
 *
 * N.B. Currently our streamconfigs API request is filtering for streams with their destination
 * event service configured as eventgate-analytics-external. However, that will likely change in
 * the future, so flexible destination event service support is added optimistically now.
 */
@Serializable
enum class DestinationEventService(val id: String, val baseUri: String) {

    ANALYTICS("eventgate-analytics-external", BuildConfig.EVENTGATE_ANALYTICS_EXTERNAL_BASE_URI),
    LOGGING("eventgate-logging-external", BuildConfig.EVENTGATE_LOGGING_EXTERNAL_BASE_URI);
}
