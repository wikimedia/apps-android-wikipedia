package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName

open class MwQueryResponse : MwResponse() {

    @SerializedName("batchcomplete")
    val batchComplete = false

    @SerializedName("continue")
    val continuation: Map<String, String>? = null
        get() = field ?: emptyMap()

    var query: MwQueryResult? = null
}
