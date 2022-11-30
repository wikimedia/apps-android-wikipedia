package org.wikipedia.search

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage

@Serializable
data class SearchResults constructor(var results: MutableList<SearchResult> = ArrayList()) {
    constructor(pages: MutableList<MwQueryPage>, wiki: WikiSite) : this() {
        // Sort the array based on the "index" property
        results.addAll(pages.sortedBy { it.index }.map { SearchResult(it, wiki) })
    }
}
