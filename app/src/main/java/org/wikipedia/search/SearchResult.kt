package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.model.BaseModel
import org.wikipedia.page.PageTitle

class SearchResult : BaseModel {
    val pageTitle: PageTitle
    val redirectFrom: String?
    val type: SearchResultType?

    enum class SearchResultType {
        SEARCH, HISTORY, READING_LIST, TAB_LIST
    }

    constructor(page: MwQueryPage, wiki: WikiSite) : this(PageTitle(page.title(),
            wiki, page.thumbUrl(), page.description(), page.displayTitle(wiki.languageCode())),
            page.redirectFrom(), SearchResultType.SEARCH)

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType) :
            this(pageTitle, null, searchResultType)

    constructor(pageTitle: PageTitle, redirectFrom: String?, searchResultType: SearchResultType) {
        this.pageTitle = pageTitle
        this.redirectFrom = redirectFrom
        type = searchResultType
    }

    override fun toString(): String {
        return pageTitle.prefixedText
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SearchResult) {
            return false
        }
        return other.pageTitle == pageTitle && other.redirectFrom.equals(redirectFrom)
    }

    override fun hashCode(): Int {
        var result = pageTitle.hashCode()
        result = 31 * result + redirectFrom.hashCode()
        return result
    }
}
