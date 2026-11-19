package org.wikipedia.search

import android.location.Location
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle

interface SearchResultCallback {
    // TODO: replace with the old onSearchAddPageToList / onSearchMovePageToList.
    fun onSearchSavePage(entry: HistoryEntry)
    fun onSearchProgressBar(enabled: Boolean)
    fun navigateToTitle(
        item: PageTitle,
        inNewTab: Boolean,
        position: Int,
        location: Location? = null,
        showSemanticSearchFeedback: Boolean = false,
        highlightText: String? = null,
        semanticSearchId: String? = null
    )

    fun setSearchText(text: String)
    fun showSemanticSearchResultsDialog(query: String)
    fun showSemanticSearchInfoDialog()
    fun onSemanticSearchFeedbackShown()
    fun onSemanticSearchIdLoaded(searchId: String)
}
