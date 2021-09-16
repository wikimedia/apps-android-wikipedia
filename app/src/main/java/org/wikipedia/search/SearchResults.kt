package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse

data class SearchResults constructor(var results: MutableList<SearchResult> = ArrayList(),
                                     var continuation: MwQueryResponse.Continuation? = null,
                                     var suggestion: String? = null) {
    constructor(pages: MutableList<MwQueryPage>, wiki: WikiSite, continuation: MwQueryResponse.Continuation?, suggestion: String?) : this() {
        // Sort the array based on the "index" property
        results.addAll(pages.sortedBy { it.index }.map { SearchResult(it, wiki) })
        this.continuation = continuation
        this.suggestion = suggestion
    }
}
