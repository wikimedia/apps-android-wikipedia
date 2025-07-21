package org.wikimedia.metricsplatform.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: make this inherit from MwResponse, and properly handle MW errors
@Serializable
class StreamConfigCollection {
    @SerialName("streams") var streamConfigs: Map<String, StreamConfig> = emptyMap()
}
