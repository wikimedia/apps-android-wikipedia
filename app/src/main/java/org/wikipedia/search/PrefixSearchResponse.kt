package org.wikipedia.search

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwQueryResponse

@Serializable
class PrefixSearchResponse : MwQueryResponse() {
    @SerializedName("searchinfo")
    private val searchInfo: SearchInfo? = null
    private val search: Search? = null

    fun suggestion(): String? {
        return searchInfo?.suggestion
    }

    internal class SearchInfo {
        @SerializedName("suggestionsnippet")
        private val snippet: String? = null
        val suggestion: String? = null
    }

    internal class Search {
        @SerializedName("ns")
        private val namespace = 0
        private val title: String? = null
    }
}
