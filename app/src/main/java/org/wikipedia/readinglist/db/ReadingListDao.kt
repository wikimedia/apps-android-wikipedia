package org.wikipedia.readinglist.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.log.L

@Dao
interface ReadingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingList(list: ReadingList): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReadingList(list: ReadingList)

    @Delete
    suspend fun deleteReadingList(list: ReadingList)

    @Query("SELECT * FROM ReadingList")
    suspend fun getListsWithoutContents(): List<ReadingList>

    @Query("SELECT * FROM ReadingList WHERE id = :id")
    suspend fun getListById(id: Long): ReadingList?

    @Query("SELECT * FROM ReadingList WHERE id IN (:readingListIds)")
    suspend fun getListsByIds(readingListIds: Set<Long>): List<ReadingList>

    @Query("UPDATE ReadingList SET remoteId = -1")
    suspend fun markAllListsUnsynced()

    @Transaction
    suspend fun getAllLists(): List<ReadingList> {
        val lists = getListsWithoutContents()
        lists.forEach {
            AppDatabase.instance.readingListPageDao().populateListPages(it)
        }
        return lists.toMutableList()
    }

    @Transaction
    suspend fun getListById(id: Long, populatePages: Boolean): ReadingList? {
        return getListById(id)?.apply {
            if (populatePages) {
                AppDatabase.instance.readingListPageDao().populateListPages(this)
            }
        }
    }

    @Transaction
    suspend fun getAllListsWithUnsyncedPages(): List<ReadingList> {
        val lists = getListsWithoutContents()
        val pages = AppDatabase.instance.readingListPageDao().getAllPagesToBeSynced()
        pages.forEach { page ->
            lists.firstOrNull { it.id == page.listId }?.apply { this.pages.add(page) }
        }
        return lists
    }

    suspend fun updateList(list: ReadingList, queueForSync: Boolean) {
        updateLists(listOf(list), queueForSync)
    }

    @Transaction
    suspend fun updateLists(lists: List<ReadingList>, queueForSync: Boolean) {
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

    suspend fun deleteList(list: ReadingList, queueForSync: Boolean = true) {
        if (list.isDefault) {
            L.w("Attempted to delete the default list.")
            return
        }
        deleteReadingList(list)
        if (queueForSync) {
            ReadingListSyncAdapter.manualSyncWithDeleteList(list)
        }
    }

    @Transaction
    suspend fun getListsFromPageOccurrences(pages: List<ReadingListPage>): List<ReadingList> {
        val lists = getListsByIds(pages.map { it.listId }.toSet())
        pages.forEach { page ->
            lists.filter { it.id == page.listId }.map { it.pages.add(page) }
        }
        return lists
    }

    @Transaction
    suspend fun createList(title: String, description: String?): ReadingList {
        if (title.isEmpty()) {
            L.w("Attempted to create list with empty title (default).")
            return getDefaultList()
        }
        return createNewList(title, description)
    }

    @Transaction
    suspend fun getDefaultList(): ReadingList {
        return getListsWithoutContents().find { it.isDefault } ?: run {
            L.w("(Re)creating default list.")
            createNewList("", L10nUtil.getString(R.string.default_reading_list_description))
        }
    }

    private suspend fun createNewList(title: String, description: String?): ReadingList {
        val protoList = ReadingList(title, description)
        protoList.id = insertReadingList(protoList)
        return protoList
    }
}
