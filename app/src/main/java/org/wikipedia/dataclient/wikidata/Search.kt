package org.wikipedia.dataclient.wikidata

import org.wikipedia.dataclient.mwapi.MwResponse

class Search : MwResponse() {

    private val success = 0
    val results: List<SearchResult>? = null
        get() = field ?: emptyList()

    class SearchResult {
        val id: String? = null
            get() = field.orEmpty()
        val label: String? = null
            get() = field.orEmpty()
        val description: String? = null
            get() = field.orEmpty()
    }
}
