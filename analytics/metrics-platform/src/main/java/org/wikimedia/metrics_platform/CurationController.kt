package org.wikimedia.metrics_platform

import org.wikimedia.metrics_platform.config.StreamConfig
import org.wikimedia.metrics_platform.event.EventProcessed

class CurationController {
    fun shouldProduceEvent(event: EventProcessed, streamConfig: StreamConfig): Boolean {
        if (!streamConfig.hasCurationFilter()) return true
        return streamConfig.curationFilter.test(event)
    }
}
