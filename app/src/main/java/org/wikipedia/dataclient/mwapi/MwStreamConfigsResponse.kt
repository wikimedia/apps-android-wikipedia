package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.StreamConfig

@Serializable
class MwStreamConfigsResponse : MwResponse() {

    @SerialName("streams")
    val streamConfigs: Map<String, StreamConfig> = emptyMap()
}
