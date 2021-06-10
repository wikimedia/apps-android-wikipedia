package org.wikipedia.history.db

import android.text.TextUtils
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import io.reactivex.rxjava3.core.Single
import org.apache.commons.lang3.StringUtils
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

@Dao
interface HistoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: HistoryEntry)

    @Query("SELECT * FROM HistoryEntry WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    fun findEntryBySearchTerm(term: String): HistoryEntry?

    @Query("DELETE FROM HistoryEntry")
    fun deleteAll(): Single<Unit>

    @Query("DELETE FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    fun deleteBy(authority: String, lang: String, namespace: String?, apiTitle: String)

    @Transaction
    fun insert(entries: List<HistoryEntry>) {
        entries.forEach {
            insertEntry(it)
        }
    }

    fun delete(entry: HistoryEntry) {
        deleteBy(entry.authority, entry.lang, entry.namespace, entry.apiTitle)
    }

    fun findHistoryItem(searchQuery: String): SearchResults {
        var normalizedQuery = StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
        if (normalizedQuery.isEmpty()) {
            return SearchResults()
        }
        normalizedQuery = normalizedQuery.replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")

        val entry = findEntryBySearchTerm("%$normalizedQuery%")
        return if (entry == null) SearchResults() else SearchResults(
            mutableListOf(
                SearchResult(
                    PageTitle(page.apiTitle, page.wiki, page.thumbUrl, page.description, page.displayTitle),
                    SearchResult.SearchResultType.READING_LIST
                )
            )
        )





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
        db.query(
            SupportSQLiteQueryBuilder.builder(PageHistoryContract.PageWithImage.TABLES)
            .columns(PageHistoryContract.PageWithImage.PROJECTION)
            .selection(selection, selectionArgs)
            .orderBy(PageHistoryContract.PageWithImage.ORDER_MRU)
            .create()).use { cursor ->
            if (cursor.moveToFirst()) {
                val indexedEntry = HistoryFragment.IndexedHistoryEntry(cursor)
                val pageTitle: PageTitle = indexedEntry.entry.title
                pageTitle.thumbUrl = indexedEntry.imageUrl
                return SearchResults(Collections.singletonList(SearchResult(pageTitle, SearchResult.SearchResultType.HISTORY)))
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
        db.query(
            SupportSQLiteQueryBuilder.builder(PageHistoryContract.PageWithImage.TABLES)
            .columns(PageHistoryContract.PageWithImage.PROJECTION)
            .selection(selection, selectionArgs)
            .orderBy(PageHistoryContract.PageWithImage.ORDER_MRU)
            .create()).use { cursor ->
            while (cursor.moveToNext()) {
                val indexedEntry = HistoryFragment.IndexedHistoryEntry(cursor)
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
