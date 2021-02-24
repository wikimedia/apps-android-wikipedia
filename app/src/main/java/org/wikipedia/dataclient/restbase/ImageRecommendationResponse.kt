package org.wikipedia.dataclient.restbase

data class ImageRecommendationResponse(val title: String, val recommendation: ImageRecommendation) {
    data class ImageRecommendation(val image: String, val rating: Float, val note: String?)
}
