package org.wikipedia.dataclient.restbase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.page.PageSummary

@JsonClass(generateAdapter = true)
class RbRelatedPages(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val pages: List<PageSummary> = emptyList()
) : MwResponse(errors, servedBy) {
    fun getPages(limit: Int): MutableList<PageSummary> {
        return pages.take(limit).toMutableList()
    }
}
