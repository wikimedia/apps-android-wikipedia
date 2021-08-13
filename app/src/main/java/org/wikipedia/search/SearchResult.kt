package org.wikipedia.search

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

data class SearchResult(val pageTitle: PageTitle,
                        val redirectFrom: String?,
                        val type: SearchResultType) {

    enum class SearchResultType {
        SEARCH, HISTORY, READING_LIST, TAB_LIST
    }

    constructor(page: MwQueryPage, wiki: WikiSite) : this(PageTitle(page.title,
            wiki, page.thumbUrl, page.description, page.getDisplayTitle(wiki.languageCode)),
            page.redirectFrom, SearchResultType.SEARCH)

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType) :
            this(pageTitle, null, searchResultType)
}
