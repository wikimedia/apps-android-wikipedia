package org.wikipedia.feed.model

import kotlinx.serialization.Serializable

@Serializable
class PlacesOfInterestCache(
    val languageCode: String,
    val latitude: Double,
    val longitude: Double,
    val anchorEpochDay: Long,
    val cards: List<PlacesOfInterestCard>
)
