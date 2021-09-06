package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.StreamConfig

@Serializable
class MwStreamConfigsResponse : MwResponse() {

    private val streams: Map<String, StreamConfig>? = null
    val streamConfigs: Map<String, StreamConfig>
        get() = streams ?: emptyMap()
}
