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
        val sroffset: Int? = null
        val gsroffset: Int? = null
        val gpsoffset: Int? = null
        @SerialName("continue") val continuation: String? = null
        @SerialName("uccontinue") val ucContinuation: String? = null
        @SerialName("rccontinue") val rcContinuation: String? = null
        @SerialName("rvcontinue") val rvContinuation: String? = null
        @SerialName("gcmcontinue") val gcmContinuation: String? = null
    }
}
