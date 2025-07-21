package org.wikimedia.metricsplatform.config

import org.wikimedia.metricsplatform.BuildConfig

/**
 * Possible event destination endpoints which can be specified in stream configurations.
 * For now, we'll assume that we always want to send to ANALYTICS.
 *
 * https://wikitech.wikimedia.org/wiki/Event_Platform/EventGate#EventGate_clusters
 */
enum class DestinationEventService(val id: String, val baseUri: String) {
    ANALYTICS("eventgate-analytics-external", BuildConfig.EVENTGATE_ANALYTICS_EXTERNAL_BASE_URI),
    LOGGING("eventgate-logging-external", BuildConfig.EVENTGATE_LOGGING_EXTERNAL_BASE_URI),
    LOCAL("eventgate-logging-local", "http://localhost:8192");
}
