package org.wikipedia.dataclient.mwapi

import org.wikipedia.analytics.eventplatform.StreamConfig

class MwStreamConfigsResponse : MwResponse() {

    private val streams: Map<String, StreamConfig>? = null
    val streamConfigs: Map<String, StreamConfig>
        get() = streams ?: emptyMap()
}