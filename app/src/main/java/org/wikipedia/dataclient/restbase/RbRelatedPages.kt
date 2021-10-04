package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary

@Serializable
class RbRelatedPages {

    val pages: List<PageSummary>? = null

    fun getPages(limit: Int): MutableList<PageSummary> {
        return pages.orEmpty().take(limit).toMutableList()
    }
}
