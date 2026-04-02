package org.wikipedia.search

import android.location.Location
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

@Serializable
data class SearchResult(val pageTitle: PageTitle,
                        val redirectFrom: String?,
                        val type: SearchResultType,
                        val coordinates: List<MwQueryPage.Coordinates>? = null,
                        val snippet: String? = null,
                        val indexInApiCall: Int = 0) {

    @Serializable
    enum class SearchResultType {
        PREFIX, FULL_TEXT, HISTORY, READING_LIST, TAB_LIST, SEMANTIC
    }

    constructor(
        page: MwQueryPage,
        wiki: WikiSite,
        coordinates: List<MwQueryPage.Coordinates>? = null,
        type: SearchResultType = SearchResultType.PREFIX,
        indexInApiCall: Int = 0
    ) : this(
        pageTitle = PageTitle(
            text = page.title,
            wiki = wiki,
            thumbUrl = page.thumbUrl(),
            description = page.description,
            displayText = page.displayTitle(wiki.languageCode),
        ).also { if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle) },
        redirectFrom = page.redirectFrom,
        type = type,
        coordinates = coordinates,
        snippet = page.snippet,
        indexInApiCall = indexInApiCall
    )

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType = SearchResultType.PREFIX, snippet: String? = null, indexInApiCall: Int = 0) :
            this(pageTitle, null, searchResultType, null, snippet, indexInApiCall)

    val location: Location? get() {
        return if (coordinates.isNullOrEmpty()) null else
            Location("").also {
                it.latitude = coordinates[0].lat
                it.longitude = coordinates[0].lon
            }
    }
}
