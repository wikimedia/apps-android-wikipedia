package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.wikipedia.analytics.eventplatform.StreamConfig

class MwStreamConfigsResponse : MwResponse() {

    @SerializedName("streams")
    val streamConfigs: Map<String, StreamConfig> = emptyMap()
}
