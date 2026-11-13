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

        @SerialName("last_updated")
        val lastUpdated: String? = null

        @SerialName("contributor_counts")
        val contributorCounts: ContributorCounts? = null

        @SerialName("reference_count")
        val referenceCount: Int? = null
    }
}

@Serializable
class ContributorCounts {
    @SerialName("total_unique") val totalUnique: Int? = null
    @SerialName("logged_in_users") val loggedInUsers: Int? = null
    @SerialName("unregistered_users") val unregisteredUsers: Int? = null
    @SerialName("known_bots") val knownBots: Int? = null
}
