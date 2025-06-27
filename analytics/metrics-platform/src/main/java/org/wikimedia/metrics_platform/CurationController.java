package org.wikimedia.metrics_platform;

import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.event.EventProcessed;

import lombok.NonNull;

public class CurationController {
    public boolean shouldProduceEvent(@NonNull EventProcessed event, @NonNull StreamConfig streamConfig) {
        if (!streamConfig.hasCurationFilter()) return true;

        return streamConfig.getCurationFilter().test(event);
    }

}
