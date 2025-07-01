package org.wikimedia.metricsplatform.config

import com.google.gson.annotations.SerializedName
import java.net.URL

/**
 * Possible event destination endpoints which can be specified in stream configurations.
 * For now, we'll assume that we always want to send to ANALYTICS.
 *
 * https://wikitech.wikimedia.org/wiki/Event_Platform/EventGate#EventGate_clusters
 */
enum class DestinationEventService(baseUri: String) {
    @SerializedName("eventgate-analytics-external")
    ANALYTICS("https://intake-analytics.wikimedia.org"),

    @SerializedName("eventgate-logging-external")
    ERROR_LOGGING("https://intake-logging.wikimedia.org"),

    @SerializedName("eventgate-logging-local")
    LOCAL("http://localhost:8192");

    private val baseUri: URL = URL("$baseUri/v1/events")

    fun getBaseUri(): URL {
        return getBaseUri(false)
    }

    fun getBaseUri(isDebug: Boolean): URL {
        return if (isDebug) this.baseUri else URL("$baseUri?hasty=true")
    }
}
