package org.wikipedia.search

import android.location.Location
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
        SEARCH, HISTORY, READING_LIST, TAB_LIST, SEMANTIC
    }

    constructor(
        page: MwQueryPage,
        wiki: WikiSite,
        coordinates: List<MwQueryPage.Coordinates>? = null,
        type: SearchResultType = SearchResultType.SEARCH
    ) : this(
        pageTitle = PageTitle(
            text = page.title,
            wiki = wiki,
            thumbUrl = page.thumbUrl(),
            description = page.description,
            displayText = page.displayTitle(wiki.languageCode)
        ),
        redirectFrom = page.redirectFrom,
        type = type,
        coordinates = coordinates
    )

    constructor(pageTitle: PageTitle, searchResultType: SearchResultType) :
            this(pageTitle, null, searchResultType)

    val location: Location? get() {
        return if (coordinates.isNullOrEmpty()) null else
            Location("").also {
                it.latitude = coordinates[0].lat
                it.longitude = coordinates[0].lon
            }
    }
}
