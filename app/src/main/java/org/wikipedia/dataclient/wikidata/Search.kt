package org.wikipedia.dataclient.wikidata

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwResponse

@Suppress("UNUSED")
class Search : MwResponse() {
    @SerializedName("search")
    val results: List<SearchResult>? = null
        get() = field ?: emptyList()
    private val success = 0

    class SearchResult {
        val id: String? = null
            get() = field ?: ""
        val label: String? = null
            get() = field ?: ""
        val description: String? = null
            get() = field ?: ""
    }
}
