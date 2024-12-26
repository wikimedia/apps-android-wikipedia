package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MwQueryResponse(
    val error: MwServiceError? = null,
    val warnings: Warnings? = null,
    val batchcomplete: Boolean = false,
    val continue_: Continue? = null,
    val query: MwQueryResult? = null
) {
    @Serializable
    data class Warnings(
        val main: Warning? = null,
        val revisions: Warning? = null,
        val query: Warning? = null
    )

    @Serializable
    data class Warning(
        val warnings: String? = null,
        @SerialName("*") val text: String? = null
    )

    @Serializable
    data class Continue(
        val sroffset: Int = 0,
        val gsroffset: Int = 0,
        val gpsoffset: Int = 0,
        val continue_: String = "",
        val grncontinue: String = "",
        val gcmcontinue: String = "",
        val rvcontinue: String = "",
        val gsrcontinue: String = ""
    )
}
