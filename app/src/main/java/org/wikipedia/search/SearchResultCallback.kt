package org.wikipedia.search

import android.location.Location
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle

interface SearchResultCallback {
    fun onSearchAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
    fun onSearchMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
    fun onSearchProgressBar(enabled: Boolean)
    fun navigateToTitle(
        item: PageTitle,
        inNewTab: Boolean,
        position: Int,
        location: Location? = null
    )

    fun setSearchText(text: String)
}
