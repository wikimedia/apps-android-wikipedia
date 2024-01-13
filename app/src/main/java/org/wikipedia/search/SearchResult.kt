package org.wikipedia.search

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

@Serializable
data class SearchResult(val pageTitle: PageTitle,
                        val redirectFrom: String?,
                        val type: SearchResultType,
                        val coordinates: List<MwQueryPage.Coordinates>? = null) {

    @Serializable
    enum class SearchResultType {
        SEARCH, HISTORY, READING_LIST, TAB_LIST
    }

    constructor(page: MwQueryPage, wiki: WikiSite, coordinates: List<MwQueryPage.Coordinates>? = null) : this(PageTitle(page.title,
            wiki, page.thumbUrl(), page.description, page.displayTitle(wiki.languageCode)),
            page.redirectFrom, SearchResultType.SEARCH, coordinates)

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType) :
            this(pageTitle, null, searchResultType)
}
