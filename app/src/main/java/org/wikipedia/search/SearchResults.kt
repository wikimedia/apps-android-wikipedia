package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage

data class SearchResults @JvmOverloads constructor(
    var results: MutableList<SearchResult> = ArrayList(),
    var continuation: Map<String, String>? = null,
    var suggestion: String? = null
) {
    constructor(pages: MutableList<MwQueryPage>, wiki: WikiSite, continuation: Map<String, String>?, suggestion: String?) : this() {
        // Sort the array based on the "index" property
        pages.sortedBy { it.index() }.mapTo(results) { SearchResult(it, wiki) }
        this.continuation = continuation
        this.suggestion = suggestion
    }
}
