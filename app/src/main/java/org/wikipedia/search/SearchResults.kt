package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage

data class SearchResults(
        var results: MutableList<SearchResult>,
        var continuation: Map<String, String>?,
        var suggestion: String? = null) {

    constructor() : this(ArrayList(), null)
    constructor(results: MutableList<SearchResult>) : this(results, null)
    constructor(pages: MutableList<MwQueryPage>, wiki: WikiSite, continuation: Map<String, String>?, suggestion: String?) : this() {
        // Sort the array based on the "index" property
        pages.sortWith { o1, o2 -> o1?.index()!!.compareTo(o2?.index()!!) }
        pages.forEach { results.add(SearchResult(it, wiki)) }
        this.continuation = continuation
        this.suggestion = suggestion
    }
}
