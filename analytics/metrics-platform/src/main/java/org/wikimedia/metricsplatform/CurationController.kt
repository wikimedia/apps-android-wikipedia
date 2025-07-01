package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.config.StreamConfig
import org.wikimedia.metricsplatform.event.EventProcessed

class CurationController {
    fun shouldProduceEvent(event: EventProcessed, streamConfig: StreamConfig): Boolean {
        if (!streamConfig.hasCurationFilter()) return true
        return streamConfig.curationFilter.test(event)
    }
}
