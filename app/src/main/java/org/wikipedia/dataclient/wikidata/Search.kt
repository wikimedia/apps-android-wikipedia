package org.wikipedia.dataclient.wikidata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class Search(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    @Json(name = "search") val results: List<SearchResult> = emptyList()
) : MwResponse(errors, servedBy) {
    @JsonClass(generateAdapter = true)
    class SearchResult(val id: String = "", val label: String = "", val description: String = "")
}
