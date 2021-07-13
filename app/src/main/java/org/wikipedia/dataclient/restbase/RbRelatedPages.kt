package org.wikipedia.dataclient.restbase

import org.wikipedia.dataclient.page.PageSummary

class RbRelatedPages {

    val pages: List<PageSummary>? = null

    fun getPages(limit: Int): MutableList<PageSummary> {
        val list = mutableListOf<PageSummary>()
        pages?.let {
            for (page in it) {
                list.add(page)
                if (limit == list.size) {
                    break
                }
            }
        }
        return list
    }
}
