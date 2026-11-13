package org.wikipedia.readinglist.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.apache.commons.lang3.StringUtils
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.offline.OfflineCacheWrite
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ArticleCacheEvidence
import org.wikipedia.readinglist.ArticleKey
import org.wikipedia.readinglist.ArticleOfflineStateStore
import org.wikipedia.readinglist.articleKey
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.mandatoryResponseUrls
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.withOfflineState
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import org.wikipedia.util.StringUtil

@Dao
interface ReadingListPageDao {
    @Query("SELECT * FROM ReadingListPage WHERE status != 2")
    suspend fun getActivePages(): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE status != 2")
    fun observeActivePages(): Flow<List<ReadingListPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingListPage(page: ReadingListPage): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReadingListPage(page: ReadingListPage)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReadingListPages(pages: List<ReadingListPage>)

    @Delete
    suspend fun deleteReadingListPage(page: ReadingListPage)

    @Query("UPDATE ReadingListPage SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: Long)

    @Transaction
    suspend fun updateRemoteIds(pages: List<ReadingListPage>) {
        pages.forEach { updateRemoteId(it.id, it.remoteId) }
    }

    @Query("UPDATE ReadingListPage SET remoteId = :remoteId, atime = :atime, mtime = :mtime WHERE id = :id")
    suspend fun updateSyncMetadata(id: Long, remoteId: Long, atime: Long, mtime: Long)

    @Query("SELECT COUNT(*) FROM ReadingListPage")
    suspend fun getPagesCount(): Int

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT lang, apiTitle, wiki, namespace FROM ReadingListPage WHERE status != :excludedStatus)")
    suspend fun getDistinctPagesCount(excludedStatus: Long): Int

    @Query("SELECT * FROM ReadingListPage WHERE id = :id")
    suspend fun getPageById(id: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE id IN (:ids)")
    suspend fun getPagesByIds(ids: Set<Long>): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE status = :status AND offline = :offline")
    suspend fun getPagesByStatus(status: Long, offline: Boolean): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE status = :status")
    suspend fun getPagesByStatus(status: Long): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND apiTitle = :apiTitle AND listId = :listId AND status != :excludedStatus")
    suspend fun getPageByParams(wiki: WikiSite, lang: String, ns: Namespace,
        apiTitle: String, listId: Long, excludedStatus: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND apiTitle = :apiTitle AND status != :excludedStatus ORDER BY id LIMIT 1")
    suspend fun getPageByParams(wiki: WikiSite, lang: String, ns: Namespace,
        apiTitle: String, excludedStatus: Long): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :ns AND apiTitle = :apiTitle AND status != :excludedStatus")
    suspend fun getPagesByParams(wiki: WikiSite, lang: String, ns: Namespace,
        apiTitle: String, excludedStatus: Long): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage ORDER BY RANDOM() DESC LIMIT :limit")
    suspend fun getPagesByRandom(limit: Int): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE listId = :listId AND status != :excludedStatus")
    suspend fun getPagesByListId(listId: Long, excludedStatus: Long): List<ReadingListPage>

    @Query("UPDATE ReadingListPage SET thumbUrl = :thumbUrl, description = :description WHERE lang = :lang AND apiTitle = :apiTitle")
    suspend fun updateThumbAndDescriptionByName(lang: String, apiTitle: String, thumbUrl: String?, description: String?)

    @Query("UPDATE ReadingListPage SET status = :newStatus WHERE status = :oldStatus AND offline = :offline")
    suspend fun updateStatus(oldStatus: Long, newStatus: Long, offline: Boolean)

    @Query("SELECT * FROM ReadingListPage WHERE lang = :lang ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPage(lang: String): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE atime BETWEEN :startMillis AND :endMillis AND status != :excludedStatus GROUP BY lang, apiTitle, wiki, namespace ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomDistinctSavedPagesBetween(startMillis: Long, endMillis: Long, excludedStatus: Long, limit: Int): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    suspend fun findPageBySearchTerm(term: String): List<ReadingListPage>

    @Query("DELETE FROM ReadingListPage WHERE status = :status")
    suspend fun deletePagesByStatus(status: Long)

    @Query("UPDATE ReadingListPage SET remoteId = -1")
    suspend fun markAllPagesUnsynced()

    @Query("SELECT * FROM ReadingListPage WHERE remoteId < 1")
    suspend fun getAllPagesToBeSynced(): List<ReadingListPage>

    @Query("SELECT COUNT(*) FROM ReadingListPage WHERE atime > 0 AND atime BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalSavedPagesBetween(startMillis: Long, endMillis: Long = System.currentTimeMillis()): Int?

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT lang, apiTitle, wiki, namespace FROM ReadingListPage WHERE atime BETWEEN :startMillis AND :endMillis AND status != :excludedStatus)")
    suspend fun getDistinctSavedPagesCountBetween(startMillis: Long, endMillis: Long, excludedStatus: Long): Int

    @Query("SELECT * FROM ReadingListPage WHERE atime > 0 AND atime > :timestamp ORDER BY atime DESC LIMIT :limit")
    suspend fun getSavedPagesSince(timestamp: Long, limit: Int): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE atime > 0 ORDER BY atime DESC LIMIT 1")
    suspend fun getMostRecentSavedPage(): ReadingListPage?

    @Query("SELECT * FROM ReadingListPage WHERE lang = :langCode GROUP BY apiTitle ORDER BY atime DESC LIMIT :limit")
    suspend fun getMostRecentSavedPagesByLang(langCode: String, limit: Int): List<ReadingListPage>

    @Query("SELECT * FROM ReadingListPage WHERE atime > 0 ORDER BY atime DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagesBySavedTime(limit: Int, offset: Int): List<ReadingListPage>

    @Query("SELECT apiTitle FROM ReadingListPage WHERE lang = :lang AND apiTitle IN (:apiTitles) AND status != :excludedStatus")
    fun observeSavedApiTitles(lang: String, apiTitles: List<String>, excludedStatus: Long): Flow<List<String>>

    suspend fun getAllPagesToBeSaved() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_SAVE, true)

    suspend fun getAllPagesToBeForcedSave() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE, true)

    @Query("SELECT * FROM ReadingListPage WHERE offline = 0 AND status != :excludedStatus AND EXISTS (SELECT 1 FROM OfflineObject WHERE usedByStr LIKE '%|' || ReadingListPage.id || '|%')")
    suspend fun getAllPagesToBeUnsaved(excludedStatus: Long = ReadingListPage.STATUS_QUEUE_FOR_DELETE): List<ReadingListPage>

    @Query("UPDATE ReadingListPage SET offline = :offline, status = :status, sizeBytes = :sizeBytes WHERE id IN (:ids) AND status != 2")
    suspend fun updateOfflineRequest(ids: List<Long>, offline: Boolean, status: Long, sizeBytes: Long)

    @Query("UPDATE ReadingListPage SET status = 2 WHERE id IN (:ids)")
    suspend fun markMembershipsForDeletion(ids: List<Long>)

    @Query("UPDATE ReadingListPage SET offline = 0 WHERE id IN (:ids) AND status != 2")
    suspend fun clearOfflineRequest(ids: List<Long>)

    @Query("UPDATE ReadingListPage SET status = 1, sizeBytes = :sizeBytes, displayTitle = COALESCE(:displayTitle, displayTitle), description = COALESCE(:description, description), thumbUrl = COALESCE(:thumbUrl, thumbUrl) WHERE id IN (:ids) AND offline = 1 AND status != 2")
    suspend fun completeOfflineDownload(ids: List<Long>, sizeBytes: Long, displayTitle: String?, description: String?, thumbUrl: String?)

    @Query("UPDATE ReadingListPage SET status = 4, sizeBytes = 0 WHERE id IN (:ids) AND offline = 1 AND status != 2")
    suspend fun failOfflineDownload(ids: List<Long>)

    suspend fun getAllPagesToBeDeleted() = getPagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_DELETE)

    suspend fun populateListPages(list: ReadingList) {
        list.pages.addAll(getPagesByListId(list.id, ReadingListPage.STATUS_QUEUE_FOR_DELETE)
            .withOfflineState(ArticleOfflineStateStore(AppDatabase.instance).snapshot()))
    }

    @Transaction
    suspend fun addPagesToList(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean) {
        for (page in pages) {
            insertPageIntoDb(list, page)
        }
        FlowEventBus.post(ArticleSavedOrDeletedEvent(true, *pages.toTypedArray()))
        SavedPageSyncService.enqueue()
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    @Transaction
    suspend fun addPagesToListIfNotExist(list: ReadingList, titles: List<PageTitle>): List<String> {
        val pages = mutableListOf<ReadingListPage>()
        for (title in titles) {
            if (getPageByTitle(list, title) != null) {
                continue
            }
            val page = addPageToList(list, title)
            pages.add(page)
        }
        if (pages.isNotEmpty()) {
            SavedPageSyncService.enqueue()
            ReadingListSyncAdapter.manualSync()
            FlowEventBus.post(ArticleSavedOrDeletedEvent(true, *pages.toTypedArray()))
        }
        return pages.map { it.displayTitle }
    }

    @Transaction
    suspend fun findPageInAnyList(title: PageTitle): ReadingListPage? {
        val members = getAllPageOccurrences(title.articleKey())
        val page = members.minByOrNull { it.id } ?: return null
        return attachArticleOfflineState(page, members)
    }

    @Transaction
    suspend fun getActivePageWithOfflineState(id: Long): ReadingListPage? {
        val page = getPageById(id)?.takeUnless { it.status == ReadingListPage.STATUS_QUEUE_FOR_DELETE } ?: return null
        return attachArticleOfflineState(page, getAllPageOccurrences(page.articleKey()))
    }

    private suspend fun attachArticleOfflineState(page: ReadingListPage, members: List<ReadingListPage>): ReadingListPage {
        val key = page.articleKey()
        val catalog = if (members.any { it.offline && it.status == ReadingListPage.STATUS_SAVED && it.sizeBytes > 0 }) {
            AppDatabase.instance.offlineObjectDao().getObjectsByUrls(key.lang, page.mandatoryResponseUrls())
        } else emptyList()
        page.articleOfflineState = ArticleOfflineStateStore.aggregate(members, catalog)[key]
        return page
    }

    suspend fun findPageForSearchQueryInAnyList(wikiSite: WikiSite, searchQuery: String): SearchResults {
        var normalizedQuery = StringUtils.stripAccents(searchQuery)
        if (normalizedQuery.isEmpty()) {
            return SearchResults()
        }
        normalizedQuery = normalizedQuery.replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")

        val pages = findPageBySearchTerm("%$normalizedQuery%")
                .filter { wikiSite.languageCode == it.lang && StringUtil.fromHtml(it.accentInvariantTitle).contains(normalizedQuery, true) }

        return if (pages.isEmpty()) SearchResults()
        else SearchResults(pages.take(2).map {
            SearchResult(PageTitle(it.apiTitle, it.wiki, it.thumbUrl, it.description, it.displayTitle), SearchResult.SearchResultType.READING_LIST)
        }.toMutableList())
    }

    @Transaction
    suspend fun markPagesForDeletion(list: ReadingList, pages: List<ReadingListPage>, queueForSync: Boolean = true) {
        preserveRemainingMemberships(pages)
        pages.map { it.id }.chunked(900).forEach { markMembershipsForDeletion(it) }
        pages.forEach { it.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE }
        if (queueForSync) {
            ReadingListSyncAdapter.manualSyncWithDeletePages(list, pages)
        }
        FlowEventBus.post(ArticleSavedOrDeletedEvent(false, *pages.toTypedArray()))
        SavedPageSyncService.enqueue()
    }

    @Transaction
    suspend fun markPagesForDeletionFromLists(lists: List<ReadingList>) {
        val pages = lists.flatMap { it.pages }
        preserveRemainingMemberships(pages)
        pages.map { it.id }.chunked(900).forEach { markMembershipsForDeletion(it) }
        pages.forEach { it.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE }
        ReadingListSyncAdapter.manualSyncWithDeletePages(lists)
        FlowEventBus.post(ArticleSavedOrDeletedEvent(false, *pages.toTypedArray()))
        SavedPageSyncService.enqueue()
    }

    @Transaction
    suspend fun markPagesForOffline(pages: List<ReadingListPage>, offline: Boolean, forcedSave: Boolean) {
        val keys = pages.mapTo(mutableSetOf()) { it.articleKey() }
        val membersByKey = getActivePages().filter { it.articleKey() in keys }.groupBy { it.articleKey() }
        val evidence = ArticleCacheEvidence(if (offline) AppDatabase.instance.offlineObjectDao().getAllObjects() else emptyList())
        membersByKey.values.forEach { members ->
            if (!offline) {
                members.map { it.id }.chunked(900).forEach { clearOfflineRequest(it) }
                OfflineCacheWrite.invalidate(members.first().articleKey())
            } else {
                requestOfflineForMembers(members, members, evidence, forcedSave)
            }
        }
        SavedPageSyncService.enqueue()
    }

    private suspend fun preserveRemainingMemberships(deleting: List<ReadingListPage>) {
        val deletingIds = deleting.mapTo(mutableSetOf()) { it.id }
        val keys = deleting.mapTo(mutableSetOf()) { it.articleKey() }
        val groups = getActivePages().filter { it.articleKey() in keys }.groupBy { it.articleKey() }
        val evidence = ArticleCacheEvidence(AppDatabase.instance.offlineObjectDao().getAllObjects())
        groups.values.forEach { members ->
            val remaining = members.filter { it.id !in deletingIds }
            if (remaining.isEmpty()) {
                OfflineCacheWrite.invalidate(members.first().articleKey())
            } else if (members.any { it.offline }) {
                requestOfflineForMembers(remaining, members, evidence,
                    members.any { it.offline && it.status == ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE }, retry = false)
            }
        }
    }

    private suspend fun requestOfflineForMembers(members: List<ReadingListPage>, sources: List<ReadingListPage>,
                                                evidence: ArticleCacheEvidence, forcedSave: Boolean, retry: Boolean = true) {
        val completed = sources.filter { evidence.hasCompletedCopy(it) }
        val size = completed.maxOfOrNull { it.sizeBytes } ?: 0L
        val status = when {
            completed.isNotEmpty() -> ReadingListPage.STATUS_SAVED
            !retry && sources.hasSettledFailure() -> ReadingListPage.STATUS_DOWNLOAD_FAILED
            forcedSave || members.any { it.offline && it.status == ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE } -> ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE
            else -> ReadingListPage.STATUS_QUEUE_FOR_SAVE
        }
        if (completed.isNotEmpty()) {
            AppDatabase.instance.offlineObjectDao().attachObjectsToPages(evidence.objects,
                completed.mapTo(mutableSetOf()) { it.id }, members.mapTo(mutableSetOf()) { it.id })
        }
        members.map { it.id }.chunked(900).forEach { updateOfflineRequest(it, true, status, size) }
    }

    suspend fun purgeDeletedPages() {
        deletePagesByStatus(ReadingListPage.STATUS_QUEUE_FOR_DELETE)
    }

    @Transaction
    suspend fun movePagesToListAndDeleteSourcePages(sourceList: ReadingList, destList: ReadingList, titles: List<PageTitle>): List<String> {
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

    @Transaction
    suspend fun movePageToList(sourceList: ReadingList, destList: ReadingList, title: PageTitle) {
        if (sourceList.id == destList.id) {
            return
        }
        val sourceReadingListPage = getPageByTitle(sourceList, title)
        if (sourceReadingListPage != null) {
            if (getPageByTitle(destList, title) == null) {
                val page = addPageToList(destList, title)
                FlowEventBus.post(ArticleSavedOrDeletedEvent(true, page))
            }
            markPagesForDeletion(sourceList, listOf(sourceReadingListPage))
            ReadingListSyncAdapter.manualSync()
            SavedPageSyncService.sendSyncEvent()
        }
    }

    suspend fun getPageByTitle(list: ReadingList, title: PageTitle): ReadingListPage? {
        return getPageByParams(
            title.wikiSite, title.wikiSite.languageCode, title.namespace(),
            title.prefixedText, list.id, ReadingListPage.STATUS_QUEUE_FOR_DELETE
        )
    }

    @Transaction
    suspend fun addPageToLists(lists: List<ReadingList>, page: ReadingListPage, queueForSync: Boolean) {
        for (list in lists) {
            if (getPageByTitle(list, ReadingListPage.toPageTitle(page)) != null) {
                continue
            }
            page.status = ReadingListPage.STATUS_QUEUE_FOR_SAVE
            insertPageIntoDb(list, page)
        }
        FlowEventBus.post(ArticleSavedOrDeletedEvent(true, page))

        SavedPageSyncService.enqueue()
        if (queueForSync) {
            ReadingListSyncAdapter.manualSync()
        }
    }

    suspend fun getAllPageOccurrences(title: PageTitle): List<ReadingListPage> {
        return getAllPageOccurrences(title.articleKey())
    }

    suspend fun getAllPageOccurrences(key: ArticleKey): List<ReadingListPage> {
        return getPagesByParams(WikiSite(key.wiki), key.lang, key.namespace, key.apiTitle, ReadingListPage.STATUS_QUEUE_FOR_DELETE)
    }

    private suspend fun addPageToList(list: ReadingList, title: PageTitle): ReadingListPage {
        val protoPage = ReadingListPage(title)
        insertPageIntoDb(list, protoPage)
        return protoPage
    }

    private fun List<ReadingListPage>.hasSettledFailure(): Boolean =
        none { it.downloadQueued } && any { it.offline && it.status == ReadingListPage.STATUS_DOWNLOAD_FAILED }

    private suspend fun insertPageIntoDb(list: ReadingList, page: ReadingListPage) {
        val members = getPagesByParams(page.wiki, page.lang, page.namespace, page.apiTitle, ReadingListPage.STATUS_QUEUE_FOR_DELETE)
        val requested = if (members.isEmpty()) page.offline else members.any { it.offline }
        val cache = AppDatabase.instance.offlineObjectDao()
        val evidence = ArticleCacheEvidence(if (requested && members.isNotEmpty()) {
            val key = page.articleKey()
            cache.getObjectsForArticle(key.wiki, key.lang, key.namespace, key.apiTitle)
        } else emptyList())
        val completed = members.filter { it.offline && evidence.hasCompletedCopy(it) }
        page.offline = requested
        page.status = when {
            requested && members.hasSettledFailure() -> ReadingListPage.STATUS_DOWNLOAD_FAILED
            requested && members.any { it.downloadQueued && it.status == ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE } ->
                ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE
            else -> ReadingListPage.STATUS_QUEUE_FOR_SAVE
        }
        page.sizeBytes = 0
        page.articleOfflineState = null
        page.listId = list.id
        page.id = 0
        page.id = insertReadingListPage(page)
        if (completed.isNotEmpty()) {
            cache.attachObjectsToPages(evidence.objects, completed.mapTo(mutableSetOf()) { it.id }, setOf(page.id))
            page.status = ReadingListPage.STATUS_SAVED
            page.sizeBytes = completed.maxOf { it.sizeBytes }
            updateOfflineRequest(listOf(page.id), requested, page.status, page.sizeBytes)
        }
    }
}
