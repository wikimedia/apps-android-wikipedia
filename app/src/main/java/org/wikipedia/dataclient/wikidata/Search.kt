package org.wikipedia.dataclient.wikidata

import org.wikipedia.dataclient.mwapi.MwResponse

class Search : MwResponse() {

    val results: List<SearchResult> = emptyList()

    class SearchResult {
        val id: String = ""
        val label: String = ""
        val description: String = ""
    }
}
