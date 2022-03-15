package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class MwQueryResponse : MwResponse() {

    val batchcomplete = true

    @SerialName("continue")
    val continuation: Continuation? = null

    var query: MwQueryResult? = null

    @Serializable
    class Continuation {
        val sroffset = 0
        val gsroffset = 0
        val gpsoffset = 0
        @SerialName("continue") val continuation: String? = null
        @SerialName("uccontinue") val ucContinuation: String? = null
        @SerialName("rvcontinue") val rvContinuation: String? = null
    }
}
