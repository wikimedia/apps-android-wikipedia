package org.wikipedia.dataclient.restbase

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.page.PageSummary

@JsonClass(generateAdapter = true)
class RbRelatedPages(errors: List<MwServiceError> = emptyList(), val pages: List<PageSummary> = emptyList()) : MwResponse(errors) {
    fun getPages(limit: Int): MutableList<PageSummary> {
        return pages.take(limit).toMutableList()
    }
}
