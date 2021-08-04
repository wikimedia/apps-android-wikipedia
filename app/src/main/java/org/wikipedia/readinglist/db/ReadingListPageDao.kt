package org.wikipedia.readinglist.db

import androidx.room.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import java.util.*

@Dao
interface ReadingListPageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReadingListPage(page: ReadingListPage): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateReadingListPage(page: ReadingListPage)

    @Query("SELECT * FROM ReadingListPage")
    fun getAllPages(): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE id = :id")
    fun getPageById(id: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE status = :status AND offline = :offline")
    fun getPagesByStatus(status: Long, offline: Boolean): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE status = :status")
    fun getPagesByStatus(status: Long): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND displayTitle = :displayTitle AND apiTitle = :apiTitle AND listId = :listId AND status != :excludedStatus")
    fun getPageByParams(wiki: WikiSite, lang: String, ns: Namespace, displayTitle: String,
        apiTitle: String, listId: Long, excludedStatus: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND displayTitle = :displayTitle AND apiTitle = :apiTitle AND status != :excludedStatus")
    fun getPageByParams(wiki: WikiSite, lang: String, ns: Namespace, displayTitle: String,
        apiTitle: String, excludedStatus: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND displayTitle = :displayTitle AND apiTitle = :apiTitle AND status != :excludedStatus")
    fun getPagesByParams(wiki: WikiSite, lang: String, ns: Namespace, displayTitle: String,
        apiTitle: String, excludedStatus: Long): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE listId = :listId AND status != :excludedStatus")
    fun getPagesByListId(listId: Long, excludedStatus: Long): List<ReadingListPage>

    @Query("UPDATE ReadingListPage SET thumbUrl = :thumbUrl, description = :description WHERE lang = :lang AND displayTitle = :displayTitle AND apiTitle = :apiTitle")
    fun updateThumbAndDescriptionByName(lang: String, displayTitle: String, apiTitle: String,
        thumbUrl: String?, description: String?)

    @Query("UPDATE ReadingListPage SET status = :newStatus WHERE status = :oldStatus AND offline = :offline")
    fun updateStatus(oldStatus: Long, newStatus: Long, offline: Boolean)

    @Query("SELECT * FROM ReadingListPage ORDER BY RANDOM() LIMIT 1")
    fun getRandomPage(): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    fun findPageBySearchTerm(term: String): List<ReadingListPage>

    @Query("DELETE FROM ReadingListPage WHERE status = :status")
    fun deletePagesByStatus(status: Long)

    @Query("UPDATE ReadingListPage SET remoteId = -1")
    fun markAllPagesUnsynced()

    @Query("SELECT * FROM ReadingListPage WHERE remoteId < 1")
    fun getAllPagesToBeSynced(): List<ReadingListPage>

    val allPagesToBeSaved
        get() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_SAVE, true)

    val allPagesToBeForcedSave
        get() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE, true)

    val allPagesToBeUnsaved
        get() = getPagesByStatus(ReadingListPage.STATUS_SAVED, false)

    val allPagesToBeDeleted
        get() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_DELETE)

    fun populateListPages(list: ReadingList) {
        list.pages.addAll(getPagesByListId(list.id, ReadingListPage.STATUS_QUEUE_FOR_DELETE))
    }

    fun addPagesToList(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean) {
        addPagesToList(list, pages)
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    @Transaction
    private fun addPagesToList(list: ReadingList, pages: List<ReadingListPage>) {
        for (page in pages) {
            insertPageIntoDb(list, page)
        }
        WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, *pages.toTypedArray()))
        SavedPageSyncService.enqueue()
    }

    @Transaction
    fun addPagesToListIfNotExist(list: ReadingList, titles: List<PageTitle>): List<String> {
        val addedTitles = mutableListOf<String>()
        for (title in titles) {
            if (getPageByTitle(list, title) != null) {
                continue
            }
            addPageToList(list, title)
            addedTitles.add(title.displayText)
        }
        if (addedTitles.isNotEmpty()) {
            SavedPageSyncService.enqueue()
            ReadingListSyncAdapter.manualSync()
        }
        return addedTitles
    }

    @Transaction
    fun updatePages(pages: List<ReadingListPage>) {
        for (page in pages) {
            updateReadingListPage(page)
        }
    }

    fun updateMetadataByTitle(pageProto: ReadingListPage, description: String?, thumbUrl: String?) {
        updateThumbAndDescriptionByName(pageProto.lang, pageProto.displayTitle, pageProto.apiTitle,
            thumbUrl, description)
    }

    fun findPageInAnyList(title: PageTitle): ReadingListPage? {
        return getPageByParams(
            title.wikiSite, title.wikiSite.languageCode(), title.namespace(),
            title.displayText, title.prefixedText, ReadingListPage.STATUS_QUEUE_FOR_DELETE
        )
    }

    fun findPageForSearchQueryInAnyList(searchQuery: String): SearchResults {
        var normalizedQuery = StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
        if (normalizedQuery.isEmpty()) {
            return SearchResults()
        }
        normalizedQuery = normalizedQuery.replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")

        val page = findPageBySearchTerm("%$normalizedQuery%")

        return if (page.isEmpty()) SearchResults()
        else SearchResults(page.take(2).map {
            SearchResult(PageTitle(it.apiTitle, it.wiki, it.thumbUrl, it.description, it.displayTitle), SearchResult.SearchResultType.READING_LIST)
        }.toMutableList())
    }

    fun pageExistsInList(list: ReadingList, title: PageTitle): Boolean {
        return getPageByTitle(list, title) != null
    }

    fun resetUnsavedPageStatus() {
        updateStatus(ReadingListPage.STATUS_SAVED, ReadingListPage.STATUS_QUEUE_FOR_SAVE, false)
    }

    @Transaction
    fun markPagesForDeletion(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean = true) {
        for (page in pages) {
            page.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE
            updateReadingListPage(page)
        }
        if (queueForSync) {
            ReadingListSyncAdapter.manualSyncWithDeletePages(list, pages)
        }
        WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(false, *pages.toTypedArray()))
        SavedPageSyncService.enqueue()
    }

    fun markPageForOffline(page: ReadingListPage, offline: Boolean, forcedSave: Boolean) {
        markPagesForOffline(listOf(page), offline, forcedSave)
    }

    @Transaction
    fun markPagesForOffline(pages: List<ReadingListPage>, offline: Boolean, forcedSave: Boolean) {
        for (page in pages) {
            if (page.offline == offline && !forcedSave) {
                continue
            }
            page.offline = offline
            if (forcedSave) {
                page.status = ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE
            }
            updateReadingListPage(page)
        }
        SavedPageSyncService.enqueue()
    }

    fun purgeDeletedPages() {
        deletePagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_DELETE)
    }

    @Transaction
    fun movePagesToListAndDeleteSourcePages(sourceList: ReadingList, destList: ReadingList, titles: List<PageTitle>): List<String> {
        val movedTitles = mutableListOf<String>()
        for (title in titles) {
            movePageToList(sourceList, destList, title)
            movedTitles.add(title.displayText)
        }
        if (movedTitles.isNotEmpty()) {
            SavedPageSyncService.enqueue()
            ReadingListSyncAdapter.manualSync()
        }
        return movedTitles
    }

    private fun movePageToList(sourceList: ReadingList, destList: ReadingList, title: PageTitle) {
        if (sourceList.id == destList.id) {
            return
        }
        val sourceReadingListPage = getPageByTitle(sourceList, title)
        if (sourceReadingListPage != null) {
            if (getPageByTitle(destList, title) == null) {
                addPageToList(destList, title)
            }
            markPagesForDeletion(sourceList, listOf(sourceReadingListPage))
            ReadingListSyncAdapter.manualSync()
            SavedPageSyncService.sendSyncEvent()
        }
    }

    fun getPageByTitle(list: ReadingList, title: PageTitle): ReadingListPage? {
        return getPageByParams(
            title.wikiSite, title.wikiSite.languageCode(), title.namespace(),
            title.displayText, title.prefixedText, list.id,
            ReadingListPage.STATUS_QUEUE_FOR_DELETE
        )
    }

    fun addPageToList(list: ReadingList, title: PageTitle, queueForSync: Boolean) {
        addPageToList(list, title)
        SavedPageSyncService.enqueue()
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    @Transaction
    fun addPageToLists(lists: List<ReadingList>, page: ReadingListPage, queueForSync: Boolean) {
        for (list in lists) {
            if (getPageByTitle(list, ReadingListPage.toPageTitle(page)) != null) {
                continue
            }
            page.status = ReadingListPage.STATUS_QUEUE_FOR_SAVE
            insertPageIntoDb(list, page)
        }
        WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, page))

        SavedPageSyncService.enqueue()
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    fun getAllPageOccurrences(title: PageTitle): List<ReadingListPage> {
        return getPagesByParams(
            title.wikiSite, title.wikiSite.languageCode(), title.namespace(),
            title.displayText, title.prefixedText, ReadingListPage.STATUS_QUEUE_FOR_DELETE
        )
    }

    private fun addPageToList(list: ReadingList, title: PageTitle) {
        val protoPage = ReadingListPage(title)
        insertPageIntoDb(list, protoPage)
        WikipediaApp.getInstance().bus.post(ArticleSavedOrDeletedEvent(true, protoPage))
    }

    private fun insertPageIntoDb(list: ReadingList, page: ReadingListPage) {
        page.listId = list.id
        page.id = insertReadingListPage(page)
    }
}
