package org.wikimedia.metrics_platform.config;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.annotations.SerializedName;

/**
 * Possible event destination endpoints which can be specified in stream configurations.
 * For now, we'll assume that we always want to send to ANALYTICS.
 *
 * https://wikitech.wikimedia.org/wiki/Event_Platform/EventGate#EventGate_clusters
 */
public enum DestinationEventService {

    @SerializedName("eventgate-analytics-external")
    ANALYTICS("https://intake-analytics.wikimedia.org"),

    @SerializedName("eventgate-logging-external")
    ERROR_LOGGING("https://intake-logging.wikimedia.org"),

    @SerializedName("eventgate-logging-local")
    LOCAL("http://localhost:8192");

    private final URL baseUri;

    DestinationEventService(String baseUri) {
        this.baseUri = new URL(baseUri + "/v1/events");
    }

    public URL getBaseUri() throws MalformedURLException {
        return getBaseUri(false);
    }

    public URL getBaseUri(boolean isDebug) throws MalformedURLException {
        return isDebug ? this.baseUri : new URL(baseUri + "?hasty=true");
    }
}
