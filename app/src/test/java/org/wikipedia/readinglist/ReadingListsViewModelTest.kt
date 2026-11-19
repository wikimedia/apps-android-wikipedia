package org.wikipedia.readinglist

import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.page.Namespace
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class ReadingListsViewModelTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val viewModelStore = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var viewModel: ReadingListsViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        mockkObject(AppDatabase.Companion, Prefs, AccountUtil, WikipediaApp.instance, SavedPageSyncService.Companion)
        every { AppDatabase.instance } returns database
        every { SavedPageSyncService.enqueue() } just Runs
        every { Prefs.isRecommendedReadingListEnabled } returns false
        every { Prefs.isReadingListLoginReminderEnabled } returns false
        every { AccountUtil.isLoggedIn } returns false
        every { AccountUtil.isTemporaryAccount } returns false
        every { AccountUtil.userName } returns ""
        every { WikipediaApp.instance.isOnline } returns false
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        database.close()
        unmockkAll()
    }

    @Test
    fun `downloaded article has the same state in all articles collection overview and detail`() = runBlocking {
        val default = page(1, 1, offline = true, status = ReadingListPage.STATUS_SAVED, title = "Deimos")
        val disabled = page(2, 2, offline = false, title = "Deimos")
        seed(default, disabled, page(3, 2, offline = false))
        cache(default)
        val row = articleRows().first { it.page.apiTitle == "Deimos" }
        assertEquals(1L, row.page.id)
        assertTrue(row.page.offline)
        assertFalse(row.page.saving)
        assertTrue(row.page.isAvailable)
        assertEquals(listOf(1L, 2L), row.containingLists.map { it.id })
        viewModel.setSelectedTab(SavedTab.COLLECTIONS)
        val collection = awaitState { it.selectedTab == SavedTab.COLLECTIONS }.rows.filterIsInstance<ReadingListRow.ListRow>().single().list
        assertEquals(1, collection.numPagesOffline)
        assertEquals(100L, collection.sizeBytesFromPages)
        val detail = viewModel.loadReadingListWithPagesById(2)!!
        assertEquals(1, detail.numPagesOffline)
        assertEquals(2, detail.pages.size)
        assertTrue(detail.pages.first { it.apiTitle == "Deimos" }.availableOffline)
        assertFalse(database.readingListPageDao().getPageById(disabled.id)!!.offline)
    }

    @Test
    fun `page actions keep the selected membership and stop when that row is deleted`() = runBlocking {
        val downloaded = page(1, 1, true, ReadingListPage.STATUS_SAVED)
        val selected = page(2, 2, false).copy(displayTitle = "Planet Earth")
        seed(downloaded, selected)
        cache(downloaded)
        val model = ReadingListsViewModel().also { viewModelStore.put("saved", it) }

        val item = model.getPageById(selected.id)!!
        assertEquals(selected.id, item.id)
        assertEquals(selected.listId, item.listId)
        assertEquals("Planet Earth", item.displayTitle)
        assertTrue(item.availableOffline)
        assertTrue(item.offlineRequested)
        assertFalse(item.offline)

        val list = database.readingListDao().getListById(selected.listId)!!
        database.readingListPageDao().markPagesForDeletion(list, listOf(selected), false)
        assertNull(model.getPageById(selected.id))
        assertTrue(model.getPageById(downloaded.id)!!.availableOffline)
    }

    @Test
    fun `metadata and identity stay stable when another occurrence finishes downloading`() = runBlocking {
        val queued = page(1, 1, true)
        val second = page(2, 2, true, status = ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE)
        seed(queued, second)
        val row = articleRows().single()
        viewModel.togglePageSelection(row.page.id)
        assertEquals(setOf(row.articleKey), viewModel.selectionState.value.selectedArticleKeys)
        val completed = second.copy(status = ReadingListPage.STATUS_SAVED, sizeBytes = 100)
        cache(completed)
        database.readingListPageDao().updateReadingListPage(completed)
        val updated = awaitState { it.rows.filterIsInstance<ReadingListRow.PageRow>().singleOrNull()?.page?.isAvailable == true }
            .rows.filterIsInstance<ReadingListRow.PageRow>().single()
        assertEquals(queued.id, updated.page.id)
        assertEquals(setOf(queued.id), viewModel.selectedPageIds())
        val deletion = viewModel.prepareSelectedPagesForDeletion()!!
        assertEquals(setOf(1L, 2L), deletion.readingLists.flatMap { it.pages }.mapTo(mutableSetOf()) { it.id })
    }

    @Test
    fun `a pending download is requested but unavailable and progresses across memberships`() = runBlocking {
        val first = page(1, 1, false)
        val pending = page(2, 2, true)
        seed(first, pending)
        val row = articleRows().single()
        assertEquals(first.id, row.page.id)
        assertTrue(row.page.offline)
        assertTrue(row.page.saving)
        assertFalse(row.page.isAvailable)
        pending.downloadProgress = 45
        viewModel.updatePageDownloadProgress(pending)
        val progress = awaitState { it.pageDownloadProgress[first.id] == 45 }
        assertEquals(45, progress.pageDownloadProgress[first.id])
    }

    @Test
    fun `zero byte saved failure and missing mandatory responses are unavailable`() = runBlocking {
        val zeroBytes = page(1, 1, true, ReadingListPage.STATUS_SAVED).copy(sizeBytes = 0)
        val noCatalog = page(2, 1, true, ReadingListPage.STATUS_SAVED, title = "Mars")
        seed(zeroBytes, noCatalog)
        cache(zeroBytes)
        val rows = articleRows()
        assertTrue(rows.all { it.page.offline && !it.page.saving && !it.page.isAvailable })
        assertEquals(0L, viewModel.loadReadingListWithPagesById(1)!!.sizeBytesFromPages)
    }

    @Test
    fun `an article is online only when all occurrences are disabled even before cleanup`() = runBlocking {
        val original = page(1, 1, true, ReadingListPage.STATUS_SAVED)
        seed(original, page(2, 2, false))
        cache(original)
        assertTrue(articleRows().single().page.isAvailable)
        database.readingListPageDao().markPagesForOffline(listOf(original), false, false)
        val row = awaitState { it.rows.filterIsInstance<ReadingListRow.PageRow>().singleOrNull()?.page?.offline == false }
            .rows.filterIsInstance<ReadingListRow.PageRow>().single()
        assertFalse(row.page.saving)
        assertFalse(row.page.isAvailable)
        assertTrue(ArticleOfflineStateStore(database).snapshot().getValue(original.articleKey()).cleanupPending)
        assertTrue(File(database.offlineObjectDao().getAllObjects().first().path + ".1").exists())
    }

    @Test
    fun `pending deletion does not provide availability`() = runBlocking {
        val deleted = page(2, 2, true, ReadingListPage.STATUS_QUEUE_FOR_DELETE)
        seed(page(1, 1, false), deleted)
        cache(deleted)
        val row = articleRows().single()
        assertFalse(row.page.offline)
        assertFalse(row.page.isAvailable)
    }

    @Test
    fun `availability and deletion respect full wiki language namespace identity`() = runBlocking {
        val original = page(1, 1, false)
        val otherWiki = original.copy(id = 2, wiki = WikiSite(Uri.parse("https://en.wiktionary.org"), "en"),
            offline = true, status = ReadingListPage.STATUS_SAVED, sizeBytes = 100)
        val otherLanguage = original.copy(id = 3, lang = "fr")
        val otherNamespace = original.copy(id = 4, namespace = Namespace.TALK)
        seed(original, otherWiki, otherLanguage, otherNamespace)
        cache(otherWiki)
        val rows = articleRows()
        assertEquals(setOf(1L, 2L, 3L, 4L), rows.mapTo(mutableSetOf()) { it.page.id })
        assertEquals(listOf(2L), rows.filter { it.page.isAvailable }.map { it.page.id })
        val deletion = viewModel.prepareSelectedPagesForDeletion(setOf(1L))!!
        assertEquals(listOf(1L), deletion.readingLists.flatMap { it.pages }.map { it.id })
    }

    @Test
    fun `adding selected articles preserves different wikis and namespaces`() = runBlocking {
        val first = page(1, 1, false)
        val otherWiki = first.copy(id = 2, wiki = WikiSite(Uri.parse("https://en.wiktionary.org"), "en"))
        val talk = first.copy(id = 3, namespace = Namespace.TALK, apiTitle = "Talk:Earth", displayTitle = "Talk:Earth")
        seed(first, otherWiki, talk, first.copy(id = 4, listId = 2))
        articleRows()
        val titles = viewModel.getPageTitlesForIds(setOf(1, 2, 3, 4))
        assertEquals(listOf("en.wikipedia.org/Earth", "en.wiktionary.org/Earth", "en.wikipedia.org/Talk:Earth"),
            titles.map { it.wikiSite.authority() + "/" + it.prefixedText })
    }

    @Test
    fun `catalog changes alone update availability and collection size`() = runBlocking {
        val page = page(1, 1, true, ReadingListPage.STATUS_SAVED)
        seed(page)
        assertFalse(articleRows().single().page.isAvailable)
        cache(page)
        awaitState { it.rows.filterIsInstance<ReadingListRow.PageRow>().singleOrNull()?.page?.isAvailable == true }
        assertEquals(100L, viewModel.loadReadingListWithPagesById(1)!!.sizeBytesFromPages)
    }

    @Test
    fun `selection stays attached to article identity through sorting and filtering`() = runBlocking {
        seed(page(1, 1, false), page(2, 1, false, title = "Mars"))
        val row = articleRows().first { it.page.apiTitle == "Earth" }
        viewModel.togglePageSelection(row.page.id)
        viewModel.setSortMode(ReadingList.SORT_BY_NAME_DESC)
        awaitState { it.sortMode == ReadingList.SORT_BY_NAME_DESC }
        assertEquals(setOf(1L), viewModel.selectedPageIds())
        viewModel.setSearchQuery("Mars")
        awaitState { it.searchQuery == "Mars" }
        assertEquals(setOf(1L), viewModel.selectedPageIds())
        assertEquals(listOf(1L), viewModel.prepareSelectedPagesForDeletion()!!.readingLists.flatMap { it.pages }.map { it.id })
    }

    @Test
    fun `search variant remains visibly selected when it has a different display title`() = runBlocking {
        val first = page(1, 1, false)
        val alias = first.copy(id = 2, listId = 2, displayTitle = "Our home planet")
        seed(first, alias)
        articleRows()
        viewModel.setSearchQuery("home")
        val row = awaitState { it.searchQuery == "home" }.rows.filterIsInstance<ReadingListRow.PageRow>().single()
        assertEquals(2L, row.page.id)
        viewModel.togglePageSelection(row.page.id)
        assertEquals(setOf(row.page.id), viewModel.selectedPageIds())
    }

    @Test
    fun `open collection detail observes both cache catalog and global OFF without reload`() = runBlocking {
        val default = page(1, 1, true, ReadingListPage.STATUS_SAVED)
        seed(default, page(2, 2, false))
        val detailModel = ReadingListFragmentViewModel()
        viewModelStore.put("detail", detailModel)
        suspend fun changed(count: Int, action: suspend () -> Unit): ReadingList {
            val result = async(Dispatchers.Unconfined) {
                withTimeout(5.seconds) {
                    detailModel.updateListByIdFlow.first { it is Resource.Success && it.data.numPagesOffline == count }
                }
            }
            action()
            while (!result.isCompleted) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(1.milliseconds)
            }
            return (result.await() as Resource.Success).data
        }
        changed(0) { detailModel.updateListById(2) }
        assertEquals(100L, changed(1) { cache(default) }.sizeBytesFromPages)
        val disabled = changed(0) { database.readingListPageDao().markPagesForOffline(listOf(default), false, false) }
        assertFalse(disabled.pages.single().offlineRequested)
        assertFalse(disabled.pages.single().availableOffline)
    }

    private suspend fun seed(vararg pages: ReadingListPage) {
        pages.map { it.listId }.distinct().forEach {
            database.readingListDao().insertReadingList(ReadingList(if (it == 1L) "" else "Planets", null, id = it))
        }
        pages.forEach { database.readingListPageDao().insertReadingListPage(it) }
    }

    private suspend fun cache(page: ReadingListPage) {
        listOf("summary", "mobile-html").forEach { endpoint ->
            val path = File(temporaryFolder.root, "${page.id}-$endpoint").path
            File("$path.0").writeText("metadata")
            File("$path.1").writeText("<p>Earth is a planet.</p>")
            database.offlineObjectDao().insertOfflineObject(OfflineObject(
                url = "${page.wiki.url()}/api/rest_v1/page/$endpoint/${page.apiTitle}", lang = page.lang, path = path, status = 0
            ).apply { addUsedBy(page.id) })
        }
    }

    private fun articleRows(): List<ReadingListRow.PageRow> = runBlocking {
        viewModel = ReadingListsViewModel()
        viewModelStore.put("saved", viewModel)
        awaitState { !it.isLoading }.rows.filterIsInstance<ReadingListRow.PageRow>()
    }

    private fun awaitState(predicate: (ReadingListsUiState) -> Boolean): ReadingListsUiState = runBlocking {
        val state = async(Dispatchers.Unconfined) {
            withTimeout(5.seconds) { viewModel.uiState.first { !it.isLoading && predicate(it) } }
        }
        while (!state.isCompleted) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(1.milliseconds)
        }
        state.await()
    }

    private fun page(id: Long, listId: Long, offline: Boolean,
        status: Long = ReadingListPage.STATUS_QUEUE_FOR_SAVE, title: String = "Earth") = ReadingListPage(
        wiki = WikiSite(Uri.parse("https://en.wikipedia.org"), "en"), namespace = Namespace.MAIN,
        displayTitle = title, apiTitle = title, id = id, listId = listId, offline = offline, status = status,
        sizeBytes = if (status == ReadingListPage.STATUS_SAVED) 100 else 0
    )
}
