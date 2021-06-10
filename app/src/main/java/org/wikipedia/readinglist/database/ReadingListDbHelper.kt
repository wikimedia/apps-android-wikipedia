package org.wikipedia.readinglist.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.database.contract.ReadingListContract
import org.wikipedia.database.contract.ReadingListPageContract
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage.Companion.toPageTitle
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter.Companion.manualSync
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter.Companion.manualSyncWithDeletePages
import org.wikipedia.savedpages.SavedPageSyncService.Companion.enqueue
import org.wikipedia.savedpages.SavedPageSyncService.Companion.sendSyncEvent
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import org.wikipedia.util.log.L
import java.util.*

object ReadingListDbHelper {

    private val readableDatabase get() = AppDatabase.getAppDatabase().readableDatabase
    private val writableDatabase get() = AppDatabase.getAppDatabase().writableDatabase

    val allListsWithUnsyncedPages: MutableList<ReadingList>
        get() {
            val lists = allListsWithoutContents
            val pages = allPagesToBeSynced
            pages.forEach { page ->
                lists.first { it.id == page.listId }.apply { this.pages.add(page) }
            }
            return lists
        }

    fun markEverythingUnsynced() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var contentValues = ContentValues()
            contentValues.put(ReadingListContract.Col.REMOTEID.name, -1)
            var result = db.update(ReadingListContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE, contentValues, null, null)
            L.d("Updated $result lists in db.")
            contentValues = ContentValues()
            contentValues.put(ReadingListPageContract.Col.REMOTEID.name, -1)
            result = db.update(ReadingListPageContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE, contentValues, null, null)
            L.d("Updated $result pages in db.")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun resetToDefaults() {
        val lists = allLists
        for (list in lists) {
            if (!list.isDefault) {
                deleteList(list, false)
            }
            markPagesForDeletion(list, list.pages, false)
        }
        // Ensure that we have a default list, in the unlikely case that it got deleted/corrupted.
        defaultList
    }










    fun getListsFromPageOccurrences(pages: List<ReadingListPage>): List<ReadingList> {
        val lists = mutableListOf<ReadingList>()
        val db = readableDatabase
        val listIds = mutableListOf<Long>()
        for (page in pages) {
            if (!listIds.contains(page.listId)) {
                listIds.add(page.listId)
            }
        }
        for (listId in listIds) {
            db.query(SupportSQLiteQueryBuilder.builder(ReadingListContract.TABLE)
                .selection(ReadingListContract.Col.ID.name + " = ?", arrayOf(listId.toString()))
                .create()).use { cursor ->
                if (cursor.moveToFirst()) {
                    lists.add(ReadingList.DATABASE_TABLE.fromCursor(cursor))
                }
            }
        }

        pages.forEach { page ->
            lists.filter { it.id == page.listId }.map { it.pages.add(page) }
        }

        return lists
    }






    fun resetUnsavedPageStatus() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val contentValues = ContentValues()
            contentValues.put(ReadingListPageContract.Col.STATUS.name, ReadingListPage.STATUS_QUEUE_FOR_SAVE)
            val result = db.update(ReadingListPageContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE, contentValues,
                    ReadingListPageContract.Col.STATUS.name + " = ? AND " +
                            ReadingListPageContract.Col.OFFLINE.name + " = ?", arrayOf(ReadingListPage.STATUS_SAVED.toString(), "0"))
            L.d("Updated $result pages in db.")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }










}
