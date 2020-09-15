package org.wikipedia.history

import android.text.TextUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.database.contract.PageHistoryContract
import org.wikipedia.history.HistoryFragment.IndexedHistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResult.SearchResultType
import org.wikipedia.search.SearchResults
import java.util.*

object HistoryDbHelper {

    fun findHistoryItem(searchQuery: String): SearchResults {
        val db = WikipediaApp.getInstance().database.readableDatabase
        val titleCol = PageHistoryContract.PageWithImage.DISPLAY_TITLE.qualifiedName()
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        var searchStr = searchQuery
        if (!TextUtils.isEmpty(searchStr)) {
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            selection = "UPPER($titleCol) LIKE UPPER(?) ESCAPE '\\'"
            selectionArgs = arrayOf("%$searchStr%")
        }
        db.query(PageHistoryContract.PageWithImage.TABLES, null,
                selection,
                selectionArgs,
                null, null, PageHistoryContract.PageWithImage.ORDER_MRU).use { cursor ->
            if (cursor.moveToFirst()) {
                val indexedEntry = IndexedHistoryEntry(cursor)
                val pageTitle: PageTitle = indexedEntry.entry.title
                pageTitle.thumbUrl = indexedEntry.imageUrl
                return SearchResults(Collections.singletonList(SearchResult(pageTitle, SearchResultType.HISTORY)))
            }
        }
        return SearchResults()
    }
}
