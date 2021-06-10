package org.wikipedia.readinglist.db

import androidx.room.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
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

    @Query("SELECT * FROM ReadingList")
    fun getListsWithoutContents(): List<ReadingList>

    @Query("SELECT * FROM ReadingList WHERE id = :id")
    fun getListById(id: Long): ReadingList?

    @Query("UPDATE ReadingList SET remoteId = -1")
    fun markAllListsUnsynced()

    fun getAllLists(): List<ReadingList> {
        val lists = getListsWithoutContents()
        lists.forEach {
            AppDatabase.getAppDatabase().readingListPageDao().populateListPages(it)
        }
        return lists.toMutableList()
    }

    fun getListById(id: Long, populatePages: Boolean): ReadingList? {
        return getListById(id)?.apply {
            if (populatePages) {
                AppDatabase.getAppDatabase().readingListPageDao().populateListPages(this)
            }
        }
    }

    fun getAllListsWithUnsyncedPages(): List<ReadingList> {
        val lists = getListsWithoutContents()
        val pages = AppDatabase.getAppDatabase().readingListPageDao().getAllPagesToBeSynced()
        pages.forEach { page ->
            lists.first { it.id == page.listId }.apply { this.pages.add(page) }
        }
        return lists
    }

    fun updateList(list: ReadingList, queueForSync: Boolean) {
        updateLists(listOf(list), queueForSync)
    }

    fun updateLists(lists: List<ReadingList>, queueForSync: Boolean) {
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

    fun getListsFromPageOccurrences(pages: List<ReadingListPage>): List<ReadingList> {
        val lists = mutableListOf<ReadingList>()
        val listIds = mutableSetOf<Long>()
        for (page in pages) {
            listIds.add(page.listId)
        }
        for (listId in listIds) {
            getListById(listId)?.let {
                lists.add(it)
            }
        }
        pages.forEach { page ->
            lists.filter { it.id == page.listId }.map { it.pages.add(page) }
        }
        return lists
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
            L.w("(Re)creating default list.")
            return createNewList("", WikipediaApp.getInstance().getString(R.string.default_reading_list_description))
        }

    private fun createNewList(title: String, description: String?): ReadingList {
        val protoList = ReadingList(title, description)
        // TODO: is the id auto-incremented properly?
        insertReadingList(protoList)
        return protoList
    }
}
