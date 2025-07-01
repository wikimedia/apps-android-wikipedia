package org.wikimedia.metricsplatform.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StreamConfigCollection {
    @SerialName("streams") var streamConfigs: MutableMap<String, StreamConfig>? = null
}
