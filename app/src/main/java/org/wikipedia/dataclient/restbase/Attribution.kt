package org.wikipedia.dataclient.restbase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class Attribution {

    @SerialName("trust_and_relevance")
    val trustAndRelevance: TrustAndRelevance? = null

    @Serializable
    class TrustAndRelevance {

        @SerialName("contributor_counts")
        val contributorCounts: Int? = null

        @SerialName("reference_count")
        val referenceCount: Int? = null
    }
}
