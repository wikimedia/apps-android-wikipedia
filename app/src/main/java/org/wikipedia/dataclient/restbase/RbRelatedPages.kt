package org.wikipedia.dataclient.restbase

import org.wikipedia.dataclient.page.PageSummary

class RbRelatedPages {

    val pages: List<PageSummary>? = null

    fun getPages(limit: Int): MutableList<PageSummary> {
        return pages.orEmpty().take(limit).toMutableList()
    }
}
