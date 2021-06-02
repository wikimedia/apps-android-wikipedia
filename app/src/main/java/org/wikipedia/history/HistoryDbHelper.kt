package org.wikipedia.history

import android.text.TextUtils
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.wikipedia.database.AppDatabase
import org.wikipedia.database.contract.PageHistoryContract
import org.wikipedia.history.HistoryFragment.IndexedHistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResult.SearchResultType
import org.wikipedia.search.SearchResults
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

object HistoryDbHelper {

    fun findHistoryItem(searchQuery: String): SearchResults {
        val db = AppDatabase.getAppDatabase().readableDatabase
        val titleCol = PageHistoryContract.PageWithImage.DISPLAY_TITLE.qualifiedName()
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        var searchStr = searchQuery
        if (!TextUtils.isEmpty(searchStr)) {
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            selection = "UPPER($titleCol) LIKE UPPER(?) ESCAPE '\\'"
            selectionArgs = arrayOf("%$searchStr%")
        }
        db.query(SupportSQLiteQueryBuilder.builder(PageHistoryContract.PageWithImage.TABLES)
            .columns(PageHistoryContract.PageWithImage.PROJECTION)
            .selection(selection, selectionArgs)
            .orderBy(PageHistoryContract.PageWithImage.ORDER_MRU)
            .create()).use { cursor ->
            if (cursor.moveToFirst()) {
                val indexedEntry = IndexedHistoryEntry(cursor)
                val pageTitle: PageTitle = indexedEntry.entry.title
                pageTitle.thumbUrl = indexedEntry.imageUrl
                return SearchResults(Collections.singletonList(SearchResult(pageTitle, SearchResultType.HISTORY)))
            }
        }
        return SearchResults()
    }

    fun filterHistoryItems(searchQuery: String): List<Any> {
        val db = AppDatabase.getAppDatabase().readableDatabase
        val titleCol = PageHistoryContract.PageWithImage.DISPLAY_TITLE.qualifiedName()
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        var searchStr = searchQuery
        if (!TextUtils.isEmpty(searchStr)) {
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            selection = "UPPER($titleCol) LIKE UPPER(?) ESCAPE '\\'"
            selectionArgs = arrayOf("%$searchStr%")
        }
        val list = ArrayList<Any>()
        db.query(SupportSQLiteQueryBuilder.builder(PageHistoryContract.PageWithImage.TABLES)
            .columns(PageHistoryContract.PageWithImage.PROJECTION)
            .selection(selection, selectionArgs)
            .orderBy(PageHistoryContract.PageWithImage.ORDER_MRU)
            .create()).use { cursor ->
            while (cursor.moveToNext()) {
                val indexedEntry = IndexedHistoryEntry(cursor)
                // Check the previous item, see if the times differ enough
                // If they do, display the section header.
                // Always do it if this is the first item.
                // Check the previous item, see if the times differ enough
                // If they do, display the section header.
                // Always do it if this is the first item.
                val curTime: String = getDateString(indexedEntry.entry.timestamp)
                val prevTime: String
                if (cursor.position != 0) {
                    cursor.moveToPrevious()
                    val prevEntry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor)
                    prevTime = getDateString(prevEntry.timestamp)
                    if (curTime != prevTime) {
                        list.add(curTime)
                    }
                    cursor.moveToNext()
                } else {
                    list.add(curTime)
                }
                list.add(indexedEntry)
            }
        }
        return list
    }

    private fun getDateString(date: Date): String {
        return DateFormat.getDateInstance().format(date)
    }
}
