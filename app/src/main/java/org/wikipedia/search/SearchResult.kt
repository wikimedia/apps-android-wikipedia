package org.wikipedia.search

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

@Serializable
data class SearchResult(val pageTitle: PageTitle,
                        val redirectFrom: String?,
                        val type: SearchResultType) {

    @Serializable
    enum class SearchResultType {
        SEARCH, HISTORY, READING_LIST, TAB_LIST
    }

    constructor(page: MwQueryPage, wiki: WikiSite) : this(PageTitle(page.title,
            wiki, page.thumbUrl(), page.description, page.displayTitle(wiki.languageCode)),
            page.redirectFrom, SearchResultType.SEARCH)

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType) :
            this(pageTitle, null, searchResultType)
}
