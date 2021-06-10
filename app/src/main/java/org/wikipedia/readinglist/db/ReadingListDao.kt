package org.wikipedia.readinglist.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import io.reactivex.rxjava3.core.Single
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.util.log.L

@Dao
interface ReadingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReadingList(list: ReadingList)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateReadingList(list: ReadingList)

    @Delete
    fun deleteReadingList(list: ReadingList)

    @Query("SELECT * FROM localreadinglist")
    fun getListsWithoutContents(): List<ReadingList>
    
    fun getAllLists(): MutableList<ReadingList> {
        val lists = getListsWithoutContents()
        lists.forEach {
            AppDatabase.getAppDatabase().readingListPageDao().populateListPages(it)
        }
        return lists.toMutableList()
    }


    fun getListById(id: Long, populatePages: Boolean): ReadingList? {
        val db = ReadingListDbHelper.readableDatabase
        var list: ReadingList? = null
        db.query(SupportSQLiteQueryBuilder.builder(ReadingListContract.TABLE)
            .selection(ReadingListContract.Col.ID.name + " = ?", arrayOf(id.toString()))
            .create()).use { cursor ->
            if (cursor.moveToFirst()) {
                list = ReadingList.DATABASE_TABLE.fromCursor(cursor)
            }
        }

        return list?.apply {
            if (populatePages) {
                populateListPages(db, this)
            }
        }
    }





    fun updateList(list: ReadingList, queueForSync: Boolean) {
        updateLists(listOf(list), queueForSync)
    }

    private fun updateLists(lists: List<ReadingList>, queueForSync: Boolean) {
        for (list in lists) {
            if (queueForSync) {
                list.dirty = true
            }
            list.touch()
            updateReadingList(list)
        }
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    fun deleteList(list: ReadingList, queueForSync: Boolean = true) {
        if (list.isDefault) {
            L.w("Attempted to delete the default list.")
            return
        }
        deleteReadingList(list)
        if (queueForSync) {
            ReadingListSyncAdapter.manualSyncWithDeleteList(list)
        }
    }




    fun createList(title: String, description: String?): ReadingList {
        if (title.isEmpty()) {
            L.w("Attempted to create list with empty title (default).")
            return defaultList
        }
        return createNewList(title, description)
    }

    val defaultList: ReadingList
        get() {
            val lists = getListsWithoutContents()
            lists.find { it.isDefault }?.run {
                return this
            }
            L.w("Recreating default list (should not happen).")
            return createNewList("", WikipediaApp.getInstance().getString(R.string.default_reading_list_description))
        }

    private fun createNewList(title: String, description: String?): ReadingList {
        val protoList = ReadingList(title, description)
        // TODO: is the id auto-incremented properly?
        insertReadingList(protoList)
        return protoList
    }
}
