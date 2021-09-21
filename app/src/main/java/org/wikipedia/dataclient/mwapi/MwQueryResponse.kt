package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class MwQueryResponse : MwResponse() {

    val batchcomplete = true

    @SerialName("continue")
    val continuation: Map<String, String> = emptyMap()

    var query: MwQueryResult? = null
}
