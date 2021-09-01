package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
open class MwQueryResponse : MwResponse() {

    val batchcomplete = true

    @SerializedName("continue")
    val continuation: Map<String, String> = emptyMap()

    var query: MwQueryResult? = null
}
