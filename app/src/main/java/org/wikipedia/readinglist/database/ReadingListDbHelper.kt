package org.wikipedia.readinglist.database

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.contract.ReadingListContract
import org.wikipedia.database.contract.ReadingListPageContract
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage.Companion.toPageTitle
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter.Companion.manualSync
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter.Companion.manualSyncWithDeleteList
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter.Companion.manualSyncWithDeletePages
import org.wikipedia.savedpages.SavedPageSyncService.Companion.enqueue
import org.wikipedia.savedpages.SavedPageSyncService.Companion.sendSyncEvent
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import org.wikipedia.util.log.L
import java.util.*

object ReadingListDbHelper {

    private val readableDatabase get() = WikipediaApp.getInstance().database.readableDatabase
    private val writableDatabase get() = WikipediaApp.getInstance().database.writableDatabase

    val allLists: MutableList<ReadingList>
        get() {
            val lists = mutableListOf<ReadingList>()
            val db = readableDatabase
            db.query(ReadingListContract.TABLE, null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val list = ReadingList.DATABASE_TABLE.fromCursor(cursor)
                    lists.add(list)
                }
            }
            lists.forEach {
                populateListPages(db, it)
            }
            return lists
        }

    val allListsWithoutContents: MutableList<ReadingList>
        get() {
            val lists = mutableListOf<ReadingList>()
            val db = readableDatabase
            db.query(ReadingListContract.TABLE, null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val list = ReadingList.DATABASE_TABLE.fromCursor(cursor)
                    lists.add(list)
                }
            }
            return lists
        }
    val allListsWithUnsyncedPages: MutableList<ReadingList>
        get() {
            val lists = allListsWithoutContents
            val pages = allPagesToBeSynced
            pages.forEach { page ->
                lists.first { it.id == page.listId }.apply { this.pages.add(page) }
            }
            return lists
        }

    fun createList(title: String, description: String?): ReadingList {
        if (title.isEmpty()) {
            L.w("Attempted to create list with empty title (default).")
            return defaultList
        }
        return createList(writableDatabase, title, description)
    }

    private fun createList(db: SQLiteDatabase, title: String, description: String?): ReadingList {
        db.beginTransaction()
        return try {
            val protoList = ReadingList(title, description)
            val id = db.insertOrThrow(ReadingListContract.TABLE, null,
                    ReadingList.DATABASE_TABLE.toContentValues(protoList))
            db.setTransactionSuccessful()
            protoList.id = id
            protoList
        } finally {
            db.endTransaction()
        }
    }

    fun updateList(list: ReadingList, queueForSync: Boolean) {
        val db = writableDatabase
        updateLists(db, listOf(list), queueForSync)
    }

    fun updateLists(lists: List<ReadingList>, queueForSync: Boolean) {
        val db = writableDatabase
        updateLists(db, lists, queueForSync)
    }

    fun updateList(db: SQLiteDatabase, list: ReadingList, queueForSync: Boolean) {
        updateLists(db, listOf(list), queueForSync)
    }

    private fun updateLists(db: SQLiteDatabase, lists: List<ReadingList>, queueForSync: Boolean) {
        db.beginTransaction()
        try {
            for (list in lists) {
                if (queueForSync) {
                    list.dirty = true
                }
                updateListInDb(db, list)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (queueForSync) {
            manualSync()
        }
    }

    @JvmOverloads
    fun deleteList(list: ReadingList, queueForSync: Boolean = true) {
        if (list.isDefault) {
            L.w("Attempted to delete the default list.")
            return
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = db.delete(ReadingListContract.TABLE,
                    ReadingListContract.Col.ID.name + " = ?", arrayOf(list.id.toString()))
            if (result != 1) {
                L.w("Failed to delete db entry for list " + list.title)
            }
            db.setTransactionSuccessful()
            if (queueForSync) {
                manualSyncWithDeleteList(list)
            }
        } finally {
            db.endTransaction()
        }
    }

    fun addPageToList(list: ReadingList, title: PageTitle, queueForSync: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            addPageToList(db, list, title)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        enqueue()
        if (queueForSync) {
            manualSync()
        }
    }

    fun addPageToLists(lists: List<ReadingList>, page: ReadingListPage, queueForSync: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (list in lists) {
                if (getPageByTitle(db, list, toPageTitle(page)) != null) {
                    continue
                }
                page.status = ReadingListPage.STATUS_QUEUE_FOR_SAVE
                insertPageInDb(db, list, page)
            }
            db.setTransactionSuccessful()
            WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, page))
        } finally {
            db.endTransaction()
        }
        enqueue()
        if (queueForSync) {
            manualSync()
        }
    }

    fun addPagesToList(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean) {
        val db = writableDatabase
        addPagesToList(db, list, pages)
        if (queueForSync) {
            manualSync()
        }
    }

    private fun addPagesToList(db: SQLiteDatabase, list: ReadingList, pages: List<ReadingListPage>) {
        db.beginTransaction()
        try {
            for (page in pages) {
                insertPageInDb(db, list, page)
            }
            db.setTransactionSuccessful()
            WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, *pages.toTypedArray()))
        } finally {
            db.endTransaction()
        }
        enqueue()
    }

    fun addPagesToListIfNotExist(list: ReadingList, titles: List<PageTitle>): List<String> {
        val db = writableDatabase
        db.beginTransaction()
        val addedTitles = mutableListOf<String>()
        try {
            for (title in titles) {
                if (getPageByTitle(db, list, title) != null) {
                    continue
                }
                addPageToList(db, list, title)
                addedTitles.add(title.displayText)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (addedTitles.isNotEmpty()) {
            enqueue()
            manualSync()
        }
        return addedTitles
    }

    fun movePagesToListAndDeleteSourcePages(sourceList: ReadingList, destList: ReadingList, titles: List<PageTitle>): List<String> {
        val db = writableDatabase
        db.beginTransaction()
        val movedTitles = mutableListOf<String>()
        try {
            for (title in titles) {
                movePageToList(db, sourceList, destList, title)
                movedTitles.add(title.displayText)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (movedTitles.isNotEmpty()) {
            enqueue()
            manualSync()
        }
        return movedTitles
    }

    private fun addPageToList(db: SQLiteDatabase, list: ReadingList, title: PageTitle) {
        val protoPage = ReadingListPage(title)
        insertPageInDb(db, list, protoPage)
        WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, protoPage))
    }

    private fun movePageToList(db: SQLiteDatabase, sourceList: ReadingList, destList: ReadingList, title: PageTitle) {
        if (sourceList.id == destList.id) {
            return
        }
        val sourceReadingListPage = getPageByTitle(db, sourceList, title)
        if (sourceReadingListPage != null) {
            if (getPageByTitle(db, destList, title) == null) {
                addPageToList(db, destList, title)
            }
            markPagesForDeletion(sourceList, listOf(sourceReadingListPage))
            manualSync()
            sendSyncEvent()
        }
    }

    @JvmOverloads
    fun markPagesForDeletion(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean = true) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (page in pages) {
                page.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE
                updatePageInDb(db, page)
            }
            db.setTransactionSuccessful()
            if (queueForSync) {
                manualSyncWithDeletePages(list, pages)
            }
            WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(false, *pages.toTypedArray()))
        } finally {
            db.endTransaction()
        }
        enqueue()
    }

    fun markPageForOffline(page: ReadingListPage, offline: Boolean, forcedSave: Boolean) {
        if (page.offline == offline && !forcedSave) {
            return
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            page.offline = offline
            if (forcedSave) {
                page.status = ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE
            }
            updatePageInDb(db, page)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        enqueue()
    }

    fun markPagesForOffline(pages: List<ReadingListPage>, offline: Boolean, forcedSave: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (page in pages) {
                if (page.offline == offline && !forcedSave) {
                    continue
                }
                page.offline = offline
                if (forcedSave) {
                    page.status = ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE
                }
                updatePageInDb(db, page)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        enqueue()
    }

    fun markEverythingUnsynced() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var result = db.update(ReadingListContract.TABLE, contentValuesOf(ReadingListContract.Col.REMOTEID.name to -1),
                    null, null)
            L.d("Updated $result lists in db.")
            result = db.update(ReadingListPageContract.TABLE, contentValuesOf(ReadingListPageContract.Col.REMOTEID.name to -1),
                    null, null)
            L.d("Updated $result pages in db.")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updatePages(pages: List<ReadingListPage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (page in pages) {
                updatePageInDb(db, page)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updatePage(page: ReadingListPage) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            updatePageInDb(db, page)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateMetadataByTitle(pageProto: ReadingListPage, description: String?,
                              thumbUrl: String?) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = db.update(ReadingListPageContract.TABLE,
                    contentValuesOf(
                            ReadingListPageContract.Col.THUMBNAIL_URL.name to thumbUrl,
                            ReadingListPageContract.Col.DESCRIPTION.name to description,
                    ),
                    ReadingListPageContract.Col.API_TITLE.name + " = ? AND " +
                            ReadingListPageContract.Col.DISPLAY_TITLE.name + " = ? AND " +
                            ReadingListPageContract.Col.LANG.name + " = ?", arrayOf(pageProto.apiTitle, pageProto.displayTitle, pageProto.lang))
            if (result != 1) {
                L.w("Failed to update db entry for page " + pageProto.displayTitle)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertPageInDb(db: SQLiteDatabase, list: ReadingList, page: ReadingListPage) {
        page.listId = list.id
        val id = db.insertOrThrow(ReadingListPageContract.TABLE, null,
                ReadingListPage.DATABASE_TABLE.toContentValues(page))
        page.id = id
    }

    private fun updatePageInDb(db: SQLiteDatabase, page: ReadingListPage) {
        val result = db.update(ReadingListPageContract.TABLE, ReadingListPage.DATABASE_TABLE.toContentValues(page),
                ReadingListPageContract.Col.ID.name + " = ?", arrayOf(page.id.toString()))
        if (result != 1) {
            L.w("Failed to update db entry for page " + page.displayTitle)
        }
    }

    private fun deletePageFromDb(db: SQLiteDatabase, page: ReadingListPage) {
        val result = db.delete(ReadingListPageContract.TABLE,
                ReadingListPageContract.Col.ID.name + " = ?", arrayOf(page.id.toString()))
        if (result != 1) {
            L.w("Failed to delete db entry for page " + page.displayTitle)
        }
    }

    private fun updateListInDb(db: SQLiteDatabase, list: ReadingList) {
        // implicitly update the last-access time of the list
        list.touch()
        val result = db.update(ReadingListContract.TABLE, ReadingList.DATABASE_TABLE.toContentValues(list),
                ReadingListContract.Col.ID.name + " = ?", arrayOf(list.id.toString()))
        if (result != 1) {
            L.w("Failed to update db entry for list " + list.title)
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

    val isEmpty: Boolean
        get() {
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " != ?", arrayOf(ReadingListPage.STATUS_QUEUE_FOR_DELETE.toInt().toString()),
                    null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    return false
                }
            }
            db.query(ReadingListContract.TABLE, null,
                    ReadingListContract.Col.TITLE.name + " != ?", arrayOf(""),
                    null, null, null).use { cursor -> return !cursor.moveToFirst() }
        }

    val randomPage: ReadingListPage?
        get() {
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " = ?", arrayOf(ReadingListPage.STATUS_SAVED.toInt().toString()),
                    null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.move(Random().nextInt(cursor.count))
                    return ReadingListPage.DATABASE_TABLE.fromCursor(cursor)
                }
            }
            return null
        }

    fun findPageInAnyList(title: PageTitle): ReadingListPage? {
        val db = readableDatabase
        db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.name + " = ? AND " +
                        ReadingListPageContract.Col.LANG.name + " = ? AND " +
                        ReadingListPageContract.Col.NAMESPACE.name + " = ? AND " + "( " +
                        ReadingListPageContract.Col.DISPLAY_TITLE.name + " = ? OR " +
                        ReadingListPageContract.Col.API_TITLE.name + " = ? ) AND " +
                        ReadingListPageContract.Col.STATUS.name + " != ?", arrayOf(title.wikiSite.authority(), title.wikiSite.languageCode(),
                title.namespace().code().toString(), title.displayText, title.prefixedText,
                ReadingListPage.STATUS_QUEUE_FOR_DELETE.toInt().toString()),
                null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                return ReadingListPage.DATABASE_TABLE.fromCursor(cursor)
            }
        }
        return null
    }

    fun findPageForSearchQueryInAnyList(searchQuery: String): SearchResults {
        val db = readableDatabase
        var normalizedQuery = StringUtils.stripAccents(searchQuery).toLowerCase(Locale.getDefault())
        val titleCol = ReadingListPageContract.Col.DISPLAY_TITLE.name
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        if (normalizedQuery.isNotEmpty()) {
            normalizedQuery = normalizedQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            selection = "UPPER($titleCol) LIKE UPPER(?) ESCAPE '\\'"
            selectionArgs = arrayOf("%$normalizedQuery%")
        }
        db.query(ReadingListPageContract.TABLE, null,
                selection,
                selectionArgs,
                null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val readingListPage = ReadingListPage.DATABASE_TABLE.fromCursor(cursor)
                return SearchResults(mutableListOf(SearchResult(PageTitle(readingListPage.apiTitle,
                        readingListPage.wiki, readingListPage.thumbUrl, readingListPage.description, readingListPage.displayTitle),
                        SearchResult.SearchResultType.READING_LIST)))
            }
        }
        return SearchResults()
    }

    fun pageExistsInList(list: ReadingList, title: PageTitle): Boolean {
        val db = readableDatabase
        return getPageByTitle(db, list, title) != null
    }

    fun getPageByTitle(list: ReadingList, title: PageTitle): ReadingListPage? {
        val db = readableDatabase
        return getPageByTitle(db, list, title)
    }

    fun getAllPageOccurrences(title: PageTitle): List<ReadingListPage> {
        val pages = mutableListOf<ReadingListPage>()
        val db = readableDatabase
        db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.name + " = ? AND " +
                        ReadingListPageContract.Col.LANG.name + " = ? AND " +
                        ReadingListPageContract.Col.NAMESPACE.name + " = ? AND " + "( " +
                        ReadingListPageContract.Col.DISPLAY_TITLE.name + " = ? OR " +
                        ReadingListPageContract.Col.API_TITLE.name + " = ? ) AND " +
                        ReadingListPageContract.Col.STATUS.name + " != ?", arrayOf(title.wikiSite.authority(), title.wikiSite.languageCode(),
                title.namespace().code().toString(), title.displayText, title.prefixedText,
                ReadingListPage.STATUS_QUEUE_FOR_DELETE.toInt().toString()),
                null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
            }
        }
        return pages
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
            db.query(ReadingListContract.TABLE, null,
                    ReadingListContract.Col.ID.name + " = ?", arrayOf(listId.toString()),
                    null, null, null).use { cursor ->
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

    fun createDefaultList(db: SQLiteDatabase): ReadingList {
        return createList(db, "",
                WikipediaApp.getInstance().getString(R.string.default_reading_list_description))
    }

    val defaultList: ReadingList
        get() {
            val lists = allListsWithoutContents
            lists.find { it.isDefault }?.run {
                return this
            }
            L.logRemoteError(RuntimeException("Recreating default list (should not happen)."))
            return createDefaultList(writableDatabase)
        }

    val allPagesToBeSaved: MutableList<ReadingListPage>
        get() {
            val pages = mutableListOf<ReadingListPage>()
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " = ? AND " +
                            ReadingListPageContract.Col.OFFLINE.name + " = ?", arrayOf(ReadingListPage.STATUS_QUEUE_FOR_SAVE.toString(), "1"),
                    null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
                }
            }
            return pages
        }

    val allPagesToBeForcedSave: MutableList<ReadingListPage>
        get() {
            val pages = mutableListOf<ReadingListPage>()
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " = ? AND " +
                            ReadingListPageContract.Col.OFFLINE.name + " = ?", arrayOf(ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE.toString(), "1"),
                    null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
                }
            }
            return pages
        }

    val allPagesToBeUnsaved: MutableList<ReadingListPage>
        get() {
            val pages = mutableListOf<ReadingListPage>()
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " = ? AND " +
                            ReadingListPageContract.Col.OFFLINE.name + " = ?", arrayOf(ReadingListPage.STATUS_SAVED.toString(), "0"),
                    null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
                }
            }
            return pages
        }

    val allPagesToBeDeleted: MutableList<ReadingListPage>
        get() {
            val pages = mutableListOf<ReadingListPage>()
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.STATUS.name + " = ?", arrayOf(ReadingListPage.STATUS_QUEUE_FOR_DELETE.toString()),
                    null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
                }
            }
            return pages
        }

    private val allPagesToBeSynced: MutableList<ReadingListPage>
        get() {
            val pages = mutableListOf<ReadingListPage>()
            val db = readableDatabase
            db.query(ReadingListPageContract.TABLE, null,
                    ReadingListPageContract.Col.REMOTEID.name + " < ?", arrayOf("1"),
                    null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
                }
            }
            return pages
        }

    fun resetUnsavedPageStatus() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = db.update(ReadingListPageContract.TABLE,
                    contentValuesOf(ReadingListPageContract.Col.STATUS.name to ReadingListPage.STATUS_QUEUE_FOR_SAVE),
                    ReadingListPageContract.Col.STATUS.name + " = ? AND " +
                            ReadingListPageContract.Col.OFFLINE.name + " = ?", arrayOf(ReadingListPage.STATUS_SAVED.toString(), "0"))
            L.d("Updated $result pages in db.")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun purgeDeletedPages() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = db.delete(ReadingListPageContract.TABLE,
                    ReadingListPageContract.Col.STATUS.name + " = ?", arrayOf(ReadingListPage.STATUS_QUEUE_FOR_DELETE.toString()))
            L.d("Deleted $result pages from db.")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getListById(id: Long, populatePages: Boolean): ReadingList? {
        val db = readableDatabase
        var list: ReadingList? = null
        db.query(ReadingListContract.TABLE, null,
                ReadingListContract.Col.ID.name + " = ?", arrayOf(id.toString()),
                null, null, null).use { cursor ->
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

    private fun populateListPages(db: SQLiteDatabase, list: ReadingList) {
        db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.LISTID.name + " = ? AND " + ReadingListPageContract.Col.STATUS.name + " != ?", arrayOf(list.id.toString(), ReadingListPage.STATUS_QUEUE_FOR_DELETE.toString()),
                null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor))
            }
        }
    }

    fun getPageById(id: Long): ReadingListPage? {
        val db = readableDatabase
        db.query(ReadingListPageContract.TABLE, null, ReadingListPageContract.Col.ID.name + " = ?", arrayOf(id.toString()), null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                return ReadingListPage.DATABASE_TABLE.fromCursor(cursor)
            }
        }
        return null
    }

    private fun getPageByTitle(db: SQLiteDatabase, list: ReadingList, title: PageTitle): ReadingListPage? {
        db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.name + " = ? AND " +
                        ReadingListPageContract.Col.LANG.name + " = ? AND " +
                        ReadingListPageContract.Col.NAMESPACE.name + " = ? AND " + "( " +
                        ReadingListPageContract.Col.DISPLAY_TITLE.name + " = ? OR " +
                        ReadingListPageContract.Col.API_TITLE.name + " = ? ) AND " +
                        ReadingListPageContract.Col.LISTID.name + " = ? AND " +
                        ReadingListPageContract.Col.STATUS.name + " != ?", arrayOf(title.wikiSite.authority(), title.wikiSite.languageCode(),
                title.namespace().code().toString(), title.displayText, title.prefixedText,
                list.id.toString(),
                ReadingListPage.STATUS_QUEUE_FOR_DELETE.toString()),
                null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                return ReadingListPage.DATABASE_TABLE.fromCursor(cursor)
            }
        }
        return null
    }
}
