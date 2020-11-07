package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;

import org.wikipedia.analytics.eventplatform.StreamConfig;

import java.util.Map;

public class MwStreamConfigsResponse extends MwResponse {

    @NonNull private Map<String, StreamConfig> streams;

    @NonNull public Map<String, StreamConfig> getStreamConfigs() {
        return streams;
    }

}
