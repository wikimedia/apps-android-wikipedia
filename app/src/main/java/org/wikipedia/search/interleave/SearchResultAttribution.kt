package org.wikipedia.search.interleave

import org.wikipedia.search.SearchResult

data class SearchResultAttribution(
    val contributorCount: Int?,
    val referenceCount: Int?
) {
    companion object {
        val NONE = SearchResultAttribution(contributorCount = null, referenceCount = null)
    }
}

data class AttributionKey(val languageCode: String, val pageId: Int)

val SearchResult.attributionKey get() = AttributionKey(pageTitle.wikiSite.languageCode, pageId)
