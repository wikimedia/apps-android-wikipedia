package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.event.EventProcessed

class CurationController {
    fun shouldProduceEvent(event: EventProcessed, streamConfig: StreamConfig): Boolean {
        if (!streamConfig.hasCurationFilter()) return true
        return streamConfig.curationFilter.test(event)
    }
}
