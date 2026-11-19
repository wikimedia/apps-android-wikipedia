package org.wikipedia.search.interleave

import org.wikipedia.search.SearchResult

data class AttributionKey(val languageCode: String, val pageId: Int)

val SearchResult.attributionKey get() = AttributionKey(pageTitle.wikiSite.languageCode, pageId)
