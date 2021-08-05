package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
open class MwQueryResponse : MwResponse() {

    @SerializedName("batchcomplete")
    val batchComplete = false

    @SerializedName("continue")
    val continuation: Map<String, String> = emptyMap()

    @Contextual var query: MwQueryResult? = null
}
