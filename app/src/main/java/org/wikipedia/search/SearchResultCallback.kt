package org.wikipedia.search

import android.location.Location
import org.wikipedia.Constants
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.search.semantic.SemanticSearchPageHooks

interface SearchResultCallback {
    fun onSearchSavePage(entry: HistoryEntry)
    fun onSearchProgressBar(enabled: Boolean)
    fun navigateToTitle(
        item: PageTitle,
        inNewTab: Boolean,
        position: Int,
        location: Location? = null,
        semanticSearchPageHooks: SemanticSearchPageHooks? = null
    )

    fun setSearchText(text: String)
    fun showSemanticSearchResultsDialog(query: String)
    fun showSemanticSearchInfoDialog()
    fun updateInvokeSource(source: Constants.InvokeSource)
}
