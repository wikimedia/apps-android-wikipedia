package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StreamConfig {

    @SerializedName("stream") @Nullable private String streamName;

    @SerializedName("schema_title") @Nullable private String schemaTitle;

    @SerializedName("topic_prefixes") @Nullable private List<String> topicPrefixes;

    @Nullable private List<String> topics;

    @SerializedName("canary_events_enabled") boolean canaryEventsEnabled;

    @SerializedName("destination_event_service") @Nullable private DestinationEventService destinationEventService;

    @SerializedName("sampling") @Nullable private SamplingConfig samplingConfig;

    @VisibleForTesting StreamConfig(@NonNull String streamName) {
        this(streamName, null);
    }

    @VisibleForTesting StreamConfig(@NonNull String streamName, @Nullable SamplingConfig samplingConfig) {
        this(streamName, samplingConfig, null);
    }

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

    @Nullable public String getStreamName() {
        return streamName;
    }

    @Nullable public String getSchemaTitle() {
        return schemaTitle;
    }

    @Nullable public List<String> getTopicPrefixes() {
        return topicPrefixes;
    }

    @Nullable public List<String> getTopics() {
        return topics;
    }

    public boolean areCanaryEventsEnabled() {
        return canaryEventsEnabled;
    }

    @Nullable public DestinationEventService getDestinationEventService() {
        return destinationEventService;
    }

    @Nullable public SamplingConfig getSamplingConfig() {
        return samplingConfig;
    }

}
