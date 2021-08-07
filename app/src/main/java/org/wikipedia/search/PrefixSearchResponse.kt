package org.wikipedia.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwQueryResponse

@JsonClass(generateAdapter = true)
class PrefixSearchResponse(@Json(name = "searchinfo") internal val searchInfo: SearchInfo? = null,
                           internal val search: Search? = null) : MwQueryResponse() {
    val suggestion: String?
        get() = searchInfo?.suggestion

    @JsonClass(generateAdapter = true)
    class SearchInfo(@Json(name = "suggestionsnippet") internal val snippet: String? = null,
                              val suggestion: String? = null)

    @JsonClass(generateAdapter = true)
    class Search(@Json(name = "ns") internal val namespace: Int = 0, internal val title: String? = null)
}
