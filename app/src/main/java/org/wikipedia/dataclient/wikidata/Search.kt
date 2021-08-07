package org.wikipedia.dataclient.wikidata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse

@JsonClass(generateAdapter = true)
class Search(@Json(name = "search") val results: List<SearchResult> = emptyList()) : MwResponse() {
    @JsonClass(generateAdapter = true)
    class SearchResult(val id: String = "", val label: String = "", val description: String = "")
}
