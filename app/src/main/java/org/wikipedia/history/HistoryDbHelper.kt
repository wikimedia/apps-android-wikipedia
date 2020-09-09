package org.wikipedia.history

import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.database.contract.PageHistoryContract
import org.wikipedia.history.HistoryFragment.IndexedHistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResult.SearchResultType
import org.wikipedia.search.SearchResults

object HistoryDbHelper {

    fun findHistoryItem(searchQuery: String): SearchResults {
        val db: SQLiteDatabase = getReadableDatabase()
        val titleCol = PageHistoryContract.PageWithImage.API_TITLE.qualifiedName()
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
                null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val searchResults = ArrayList<SearchResult>()
                val indexedEntry = IndexedHistoryEntry(cursor)
                val pageTitle: PageTitle = indexedEntry.entry.title
                pageTitle.thumbUrl = indexedEntry.imageUrl
                val searchResult = SearchResult(SearchResultType.HISTORY_SEARCH_RESULT, pageTitle)
                searchResults.add(searchResult)
                return SearchResults(searchResults)
            }
        }
        return SearchResults()
    }

    private fun getReadableDatabase(): SQLiteDatabase {
        return WikipediaApp.getInstance().database.readableDatabase
    }
}
