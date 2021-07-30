package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.analytics.eventplatform.StreamConfig;

import java.util.Collections;
import java.util.Map;

import kotlinx.serialization.Serializable;

@Serializable
public class MwStreamConfigsResponse extends MwResponse {

    @Nullable private Map<String, StreamConfig> streams;

    @NonNull public Map<String, StreamConfig> getStreamConfigs() {
        return streams != null ? streams : Collections.emptyMap();
    }

}
