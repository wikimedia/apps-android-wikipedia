package org.wikipedia.dataclient.restbase

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.PageSummary

@JsonClass(generateAdapter = true)
class RbRelatedPages(val pages: List<PageSummary> = emptyList()) {
    fun getPages(limit: Int): MutableList<PageSummary> {
        return pages.take(limit).toMutableList()
    }
}
