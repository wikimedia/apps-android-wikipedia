package org.wikipedia.dataclient.restbase

data class ImageRecommendationResponse(val pageId: Int, val image: String, val foundOnWikis: List<String>) {
    var pageTitle: String = ""
}
