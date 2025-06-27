package org.wikimedia.metrics_platform.config;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SourceConfig {
    private final Map<String, StreamConfig> streamConfigs;
    private final Set<StreamConfig> sourceConfigs;

    public SourceConfig(Map<String, StreamConfig> streamConfigs) {
        this.streamConfigs = unmodifiableMap(streamConfigs);
        this.sourceConfigs = unmodifiableSet(new HashSet<>(streamConfigs.values()));
    }

    /**
     * Get all stream configs' stream names.
     */
    public Set<String> getStreamNames() {
        Set<String> sourceConfigNamesSet = streamConfigs.keySet();
        return unmodifiableSet(sourceConfigNamesSet);
    }

    /**
     * Get all stream configs with stream name.
     */
    public Map<String, StreamConfig> getStreamConfigsMap() {
        return streamConfigs;
    }

    /**
     * Get all stream configs without stream name.
     */
    public Set<StreamConfig> getStreamConfigsRaw() {
        return sourceConfigs;
    }

    /**
     * Get stream config by stream name.
     *
     * @param streamName stream name
     */
    public StreamConfig getStreamConfigByName(String streamName) {
        return streamConfigs.get(streamName);
    }

    /**
     * Get stream names by event name.
     *
     * @param eventName event name
     */
    public Set<String> getStreamNamesByEvent(String eventName) {
        return sourceConfigs.stream()
                .filter(streamConfig -> streamConfig.isInterestedInEvent(eventName))
                .map(StreamConfig::getStreamName)
                .collect(toSet());
    }

}
