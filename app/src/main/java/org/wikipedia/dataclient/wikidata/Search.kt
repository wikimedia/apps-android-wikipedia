package org.wikipedia.dataclient.wikidata

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwResponse

class Search : MwResponse() {

    @SerializedName("search")
    val results: List<SearchResult> = emptyList()

    class SearchResult {
        val id: String = ""
        val label: String = ""
        val description: String = ""
    }
}
