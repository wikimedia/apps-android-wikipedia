package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage

data class SearchResults(
        var results: List<SearchResult>,
        var continuation: Map<String, String>?,
        var suggestion: String? = null) {

    constructor() : this(ArrayList(), null)

    constructor(results: List<SearchResult>) : this(results, null)

    constructor(pages: List<MwQueryPage>, wiki: WikiSite,
                continuation: Map<String, String>?, suggestion: String?) : this() {
        val searchResults: MutableList<SearchResult> = ArrayList()

        // Sort the array based on the "index" property
        (pages as ArrayList).sortWith { o1, o2 -> (o1?.index()!!.compareTo(o2?.index()!!)) }

        for (page in pages) {
            searchResults.add(SearchResult(page, wiki))
        }

        results = searchResults
        this.continuation = continuation
        this.suggestion = suggestion
    }
}
