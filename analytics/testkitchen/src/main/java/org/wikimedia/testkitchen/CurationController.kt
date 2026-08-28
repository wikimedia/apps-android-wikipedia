package org.wikimedia.testkitchen

import org.wikimedia.testkitchen.config.StreamConfig
import org.wikimedia.testkitchen.event.Event

class CurationController {
    fun shouldProduceEvent(event: Event, streamConfig: StreamConfig): Boolean {
        if (!streamConfig.hasCurationFilter()) return true
        return streamConfig.curationFilter.test(event)
    }
}
