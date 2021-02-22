package org.wikipedia.search

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwQueryResponse

class PrefixSearchResponse : MwQueryResponse() {
    @SerializedName("searchinfo")
    private val searchInfo: SearchInfo? = null
    private val search: Search? = null

    fun suggestion(): String? {
        return searchInfo?.suggestion
    }

    internal class SearchInfo {
        val suggestion: String? = null

        @SerializedName("suggestionsnippet")
        private val snippet: String? = null
    }

    internal class Search {
        @SerializedName("ns")
        private val namespace = 0
        private val title: String? = null
    }
}
