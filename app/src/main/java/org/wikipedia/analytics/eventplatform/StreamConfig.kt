package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class StreamConfig {

    @SerializedName("stream") @Nullable private String streamName;

    @SerializedName("schema_title") @Nullable private String schemaTitle;

    @SerializedName("topic_prefixes") @Nullable private List<String> topicPrefixes;

    @Nullable private List<String> topics;

    @SerializedName("canary_events_enabled") boolean canaryEventsEnabled;

    @SerializedName("destination_event_service") @Nullable private DestinationEventService destinationEventService;

    @SerializedName("sampling") @Nullable private SamplingConfig samplingConfig;

    /**
     * Constructor for testing.
     *
     * In practice, field values will be set by Gson during deserialization using reflection.
     *
     * @param streamName stream name
     * @param destinationEventService destination
     */
    @VisibleForTesting StreamConfig(
            @NonNull String streamName,
            @Nullable SamplingConfig samplingConfig,
            @Nullable DestinationEventService destinationEventService
    ) {
        this.streamName = streamName;
        this.samplingConfig = samplingConfig;
        this.destinationEventService = destinationEventService;
    }

    @NonNull public String getStreamName() {
        return StringUtils.defaultString(streamName);
    }

    @NonNull public String getSchemaTitle() {
        return StringUtils.defaultString(schemaTitle);
    }

    @NonNull public List<String> getTopicPrefixes() {
        return topicPrefixes != null ? topicPrefixes : Collections.emptyList();
    }

    @NonNull public List<String> getTopics() {
        return topics != null ? topics : Collections.emptyList();
    }

    public boolean areCanaryEventsEnabled() {
        return canaryEventsEnabled;
    }

    @NonNull public DestinationEventService getDestinationEventService() {
        return destinationEventService != null ? destinationEventService : DestinationEventService.ANALYTICS;
    }

    @Nullable public SamplingConfig getSamplingConfig() {
        return samplingConfig;
    }

}
