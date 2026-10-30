package org.wikipedia.feed.model

import kotlinx.serialization.Serializable

@Serializable
class SeeAllRecommendationCard : ForYouCard() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
