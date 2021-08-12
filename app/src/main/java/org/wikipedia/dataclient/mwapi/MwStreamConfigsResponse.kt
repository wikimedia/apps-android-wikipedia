package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.StreamConfig

@Serializable
class MwStreamConfigsResponse : MwResponse() {

    @SerializedName("streams")
    val streamConfigs: Map<String, StreamConfig> = emptyMap()
}
