package org.wikipedia.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwQueryResponse

@Serializable
class PrefixSearchResponse : MwQueryResponse() {
    @SerialName("searchinfo")
    private val searchInfo: SearchInfo? = null
    private val search: Search? = null

    fun suggestion(): String? {
        return searchInfo?.suggestion
    }

    @Serializable
    internal class SearchInfo {
        @SerialName("suggestionsnippet")
        private val snippet: String? = null
        val suggestion: String? = null
    }

    @Serializable
    internal class Search {
        @SerialName("ns")
        private val namespace = 0
        private val title: String? = null
    }
}
