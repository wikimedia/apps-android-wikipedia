package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;

import com.squareup.moshi.Json;

import org.wikipedia.BuildConfig;

/**
 * Possible event destination endpoints which can be specified in stream configurations.
 * https://wikitech.wikimedia.org/wiki/Event_Platform/EventGate#EventGate_clusters
 *
 * N.B. Currently our streamconfigs API request is filtering for streams with their destination
 * event service configured as eventgate-analytics-external. However, that will likely change in
 * the future, so flexible destination event service support is added optimistically now.
 */
public enum DestinationEventService {

    @Json(name = "eventgate-analytics-external") ANALYTICS (
            "eventgate-analytics-external",
            BuildConfig.EVENTGATE_ANALYTICS_EXTERNAL_BASE_URI
    ),

    @Json(name = "eventgate-logging-external") LOGGING (
            "eventgate-logging-external",
            BuildConfig.EVENTGATE_LOGGING_EXTERNAL_BASE_URI
    );

    @NonNull private final String id;
    @NonNull private final String baseUri;

    DestinationEventService(@NonNull String id, @NonNull String baseUri) {
        this.id = id;
        this.baseUri = baseUri;
    }

    @NonNull public String getId() {
        return id;
    }

    @NonNull public String getBaseUri() {
        return baseUri;
    }

}
