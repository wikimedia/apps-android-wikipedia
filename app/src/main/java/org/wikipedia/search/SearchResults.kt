package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage

data class SearchResults constructor(var results: MutableList<SearchResult> = ArrayList(),
                                     var continuation: Map<String, String> = emptyMap(),
                                     var suggestion: String? = null) {
    constructor(pages: MutableList<MwQueryPage>, wiki: WikiSite, continuation: Map<String, String>, suggestion: String?) : this() {
        // Sort the array based on the "index" property
        results.addAll(pages.sortedBy { it.index }.map { SearchResult(it, wiki) })
        this.continuation = continuation
        this.suggestion = suggestion
    }
}
