package org.wikipedia.offline

import android.net.Uri
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.room.Room
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
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
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.articleKey
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.savedpages.SavedPageSyncService
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ReadingListOfflineTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private val title get() = PageTitle("Saturn", WikiSite.forLanguageCode("en"))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mockkObject(AppDatabase.Companion, SavedPageSyncService.Companion, ReadingListSyncAdapter.Companion)
        every { AppDatabase.instance } returns database
        every { SavedPageSyncService.enqueue() } just Runs
        every { ReadingListSyncAdapter.manualSyncWithDeletePages(any<List<ReadingList>>()) } just Runs
    }

    @After
    fun tearDown() {
        database.close()
        unmockkAll()
    }

    @Test
    fun `global downloaded Deimos is available in every collection and detail without rewriting local intent`() = runBlocking {
        val defaultPage = savedPage(ReadingList("", null)).copy(apiTitle = "Deimos", displayTitle = "Deimos", sizeBytes = 4_972_626)
        database.readingListPageDao().updateReadingListPage(defaultPage)
        val collectionPage = savedPage(ReadingList("Mars", null)).copy(apiTitle = "Deimos", displayTitle = "Deimos",
            offline = false, status = ReadingListPage.STATUS_QUEUE_FOR_SAVE, sizeBytes = 4_972_626)
        database.readingListPageDao().updateReadingListPage(collectionPage)
        val earth = collectionPage.copy(id = 0, apiTitle = "Earth", displayTitle = "Earth", sizeBytes = 0)
        database.readingListPageDao().insertReadingListPage(earth)
        listOf("summary", "mobile-html").forEach { endpoint ->
            val path = File(temporaryFolder.root, endpoint).path
            File("$path.0").writeText("metadata")
            File("$path.1").writeText(if (endpoint == "mobile-html") "<p>Deimos is a moon of Mars.</p>" else "{\"title\":\"Deimos\"}")
            database.offlineObjectDao().insertOfflineObject(OfflineObject(
                url = "https://en.wikipedia.org/api/rest_v1/page/$endpoint/Deimos", lang = "en", path = path, status = 0
            ).apply { addUsedBy(defaultPage.id) })
        }

        val detail = database.readingListDao().getListById(collectionPage.listId, true)!!

        assertEquals(2, detail.pages.size)
        assertEquals(1, detail.numPagesOffline)
        assertEquals(4_972_626L, detail.sizeBytesFromPages)
        assertFalse(database.readingListPageDao().getPageById(collectionPage.id)!!.offline)
    }

    @Test
    fun `removing collection offline releases default ownership without removing saved articles`() = runBlocking {
        val defaultPage = savedPage(ReadingList("", null, id = 17))
        val collectionPage = savedPage(ReadingList("Planets", null, id = 42))
        val contents = cachedObject(defaultPage, collectionPage)

        database.readingListPageDao().markPagesForOffline(listOf(collectionPage), offline = false, forcedSave = false)
        removeOfflineObjects()

        val savedPages = database.readingListPageDao().getAllPageOccurrences(title)
        assertEquals(2, savedPages.size)
        assertFalse(savedPages.any { it.offline })
        assertNull(database.offlineObjectDao().findObject("https://en.wikipedia.org/Saturn", "en"))
        assertFalse(contents.exists())
        assertFalse(File(contents.path.removeSuffix(".1") + ".0").exists())
    }

    @Test
    fun `disabled queued articles release leftover offline objects`() = runBlocking {
        val page = savedPage(ReadingList("Planets", null)).apply {
            offline = false
            status = ReadingListPage.STATUS_QUEUE_FOR_SAVE
        }
        database.readingListPageDao().updateReadingListPage(page)
        val contents = cachedObject(page)

        removeOfflineObjects()

        assertFalse(contents.exists())
        assertNull(database.offlineObjectDao().findObject("https://en.wikipedia.org/Saturn", "en"))
        assertEquals(1, database.readingListPageDao().getAllPageOccurrences(title).size)
    }

    @Test
    fun `cache associates only offline enabled owners`() = runBlocking {
        val disabledPage = savedPage(ReadingList("", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(disabledPage)
        val enabledPage = savedPage(ReadingList("Planets", null))
        val path = File(temporaryFolder.root, "download").path
        File("$path.1").writeText("article contents")

        File("$path.0").writeText("metadata")
        OfflineCacheWrite.start(title.articleKey()).use { write ->
            database.offlineObjectDao().publishResponse(OfflineObject(url = "https://en.wikipedia.org/Saturn", lang = "en", path = path, status = 0),
                File(temporaryFolder.root, "published").path, write, sharedImage = false)
        }

        assertEquals(listOf(enabledPage.id), database.offlineObjectDao().findObject("https://en.wikipedia.org/Saturn", "en")!!.usedBy)
    }

    @Test
    fun `removing offline from any collection disables every membership and releases shared files`() = runBlocking {
        val defaultPage = savedPage(ReadingList("", null))
        val first = savedPage(ReadingList("Planets", null))
        val second = savedPage(ReadingList("Astronomy", null))
        val contents = cachedObject(defaultPage, first, second)

        database.readingListPageDao().markPagesForOffline(listOf(first), offline = false, forcedSave = false)
        removeOfflineObjects()

        assertFalse(database.readingListPageDao().getAllPageOccurrences(title).any { it.offline })
        assertFalse(contents.exists())
        assertEquals(3, database.readingListPageDao().getAllPageOccurrences(title).size)
    }

    @Test
    fun `opening a saved online only article does not request offline caching`() = runBlocking {
        val page = savedPage(ReadingList("Planets", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(page)

        assertNull(requestedSaveHeader())
    }

    @Test
    fun `opening an article uses any offline collection rather than the first saved occurrence`() = runBlocking {
        val first = savedPage(ReadingList("", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(first)
        savedPage(ReadingList("Planets", null))

        assertEquals(OfflineCacheInterceptor.SAVE_HEADER_SAVE, requestedSaveHeader())
    }

    @Test
    fun `enabling offline again queues every membership of the selected article`() = runBlocking {
        val defaultPage = savedPage(ReadingList("", null))
        val page = savedPage(ReadingList("Planets", null))
        database.readingListPageDao().markPagesForOffline(listOf(page), offline = false, forcedSave = false)

        database.readingListPageDao().markPagesForOffline(listOf(page), offline = true, forcedSave = false)

        assertEquals(setOf(defaultPage.id, page.id), database.readingListPageDao().getAllPagesToBeSaved().mapTo(mutableSetOf()) { it.id })
        assertTrue(database.readingListPageDao().getPageById(defaultPage.id)!!.offline)
        assertEquals(2, database.readingListPageDao().getAllPageOccurrences(title).size)
    }

    @Test
    fun `disabling offline preserves default entries of other titles wikis languages and namespaces`() = runBlocking {
        val defaultPage = savedPage(ReadingList("", null, id = 17))
        val page = savedPage(ReadingList("Planets", null, id = 42))
        val unrelated = listOf(
            defaultPage.copy(id = 0, apiTitle = "Moon"),
            defaultPage.copy(id = 0, wiki = WikiSite("https://en.wiktionary.org", "en")),
            defaultPage.copy(id = 0, lang = "fr"),
            defaultPage.copy(id = 0, namespace = Namespace.TALK)
        ).map { database.readingListPageDao().insertReadingListPage(it) }

        database.readingListPageDao().markPagesForOffline(listOf(page), offline = false, forcedSave = false)

        assertFalse(database.readingListPageDao().getPageById(defaultPage.id)!!.offline)
        unrelated.forEach { assertTrue(database.readingListPageDao().getPageById(it)!!.offline) }
    }

    @Test
    fun `pending deletion does not qualify an online only saved article for offline caching`() = runBlocking {
        val disabled = savedPage(ReadingList("", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(disabled)
        val deleted = savedPage(ReadingList("Planets", null)).apply { status = ReadingListPage.STATUS_QUEUE_FOR_DELETE }
        database.readingListPageDao().updateReadingListPage(deleted)

        assertNull(requestedSaveHeader())
    }

    @Test
    fun `adding to another collection after global OFF preserves OFF without downloading`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        database.readingListPageDao().markPagesForOffline(listOf(first), false, false)
        val secondList = ReadingList("Astronomy", null)
        secondList.id = database.readingListDao().insertReadingList(secondList)

        database.readingListPageDao().addPagesToList(secondList, listOf(ReadingListPage(title)), false)

        val occurrences = database.readingListPageDao().getAllPageOccurrences(title)
        assertEquals(2, occurrences.size)
        assertFalse(occurrences.any { it.offline })
        assertTrue(database.readingListPageDao().getAllPagesToBeSaved().isEmpty())
    }

    @Test
    fun `adding a downloaded article then deleting the original membership preserves readable HTML`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        val contents = cachedArticle(first)
        val secondList = ReadingList("Astronomy", null)
        secondList.id = database.readingListDao().insertReadingList(secondList)
        database.readingListPageDao().addPagesToList(secondList, listOf(ReadingListPage(title)), false)
        val added = database.readingListPageDao().getPagesByListId(secondList.id, ReadingListPage.STATUS_QUEUE_FOR_DELETE).single()
        assertEquals(ReadingListPage.STATUS_SAVED, added.status)
        assertEquals(100L, added.sizeBytes)
        assertTrue(database.offlineObjectDao().getAllObjects().all { added.id in it.usedBy })
        first.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE
        database.readingListPageDao().updateReadingListPage(first)

        removeOfflineObjectsFor(listOf(first.id))
        database.readingListPageDao().purgeDeletedPages()

        assertTrue(contents.all { it.exists() })
        val detail = database.readingListDao().getListById(secondList.id, true)!!
        assertEquals(1, detail.numPagesOffline)
        assertEquals("<p>Saturn has rings.</p>", readOfflineHtml())
        added.status = ReadingListPage.STATUS_QUEUE_FOR_DELETE
        database.readingListPageDao().updateReadingListPage(added)
        removeOfflineObjectsFor(listOf(added.id))
        assertFalse(contents.any { it.exists() })
    }

    @Test
    fun `ON preserves completed copy and normalizes every owner without queuing another download`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        val second = savedPage(ReadingList("Astronomy", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(second)
        val contents = cachedArticle(first)

        database.readingListPageDao().markPagesForOffline(listOf(second), true, true)

        val occurrences = database.readingListPageDao().getAllPageOccurrences(title)
        assertTrue(occurrences.all { it.offline && it.status == ReadingListPage.STATUS_SAVED && it.sizeBytes == 100L })
        assertTrue(database.readingListPageDao().getAllPagesToBeForcedSave().isEmpty())
        assertTrue(database.offlineObjectDao().getAllObjects().all { it.usedBy.toSet() == setOf(first.id, second.id) })
        assertTrue(contents.all { it.exists() })
    }

    @Test
    fun `explicit network override promotes existing pending request across collections`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null)).apply { status = ReadingListPage.STATUS_QUEUE_FOR_SAVE }
        val second = savedPage(ReadingList("Astronomy", null)).apply { status = ReadingListPage.STATUS_QUEUE_FOR_SAVE }
        database.readingListPageDao().updateReadingListPages(listOf(first, second))

        database.readingListPageDao().markPagesForOffline(listOf(first), true, true)

        assertEquals(setOf(first.id, second.id), database.readingListPageDao().getAllPagesToBeForcedSave().mapTo(mutableSetOf()) { it.id })
        assertTrue(database.readingListPageDao().getAllPageOccurrences(title).all { it.offline })
    }

    @Test
    fun `collection bulk changes only its distinct article keys across all memberships`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        val second = savedPage(ReadingList("Astronomy", null))
        val unrelated = second.copy(id = 0, apiTitle = "Mars", displayTitle = "Mars")
        unrelated.id = database.readingListPageDao().insertReadingListPage(unrelated)

        database.readingListPageDao().markPagesForOffline(listOf(first, first), false, false)

        assertFalse(database.readingListPageDao().getPageById(first.id)!!.offline)
        assertFalse(database.readingListPageDao().getPageById(second.id)!!.offline)
        assertTrue(database.readingListPageDao().getPageById(unrelated.id)!!.offline)
    }

    @Test
    fun `global commands handle more memberships than one SQLite argument batch`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        repeat(1005) { index -> database.readingListPageDao().insertReadingListPage(first.copy(id = 0, listId = 100L + index)) }

        database.readingListPageDao().markPagesForOffline(listOf(first), false, false)
        assertEquals(1006, database.readingListPageDao().getAllPageOccurrences(title).size)
        assertFalse(database.readingListPageDao().getAllPageOccurrences(title).any { it.offline })
        database.readingListPageDao().markPagesForOffline(listOf(first), true, true)
        assertEquals(1006, database.readingListPageDao().getAllPagesToBeForcedSave().size)
    }

    @Test
    fun `deleting the sole downloaded membership of a legacy mixed article retains its other membership and copy`() = runBlocking {
        val first = savedPage(ReadingList("Planets", null))
        val second = savedPage(ReadingList("Astronomy", null)).apply { offline = false }
        database.readingListPageDao().updateReadingListPage(second)
        val contents = cachedArticle(first)

        database.readingListPageDao().markPagesForDeletion(ReadingList("Planets", null, id = first.listId), listOf(first), false)
        removeOfflineObjectsFor(listOf(first.id))
        database.readingListPageDao().purgeDeletedPages()

        val remaining = database.readingListDao().getListById(second.listId, true)!!
        assertEquals(1, remaining.numPagesOffline)
        assertTrue(contents.all { it.exists() })
        assertEquals("<p>Saturn has rings.</p>", readOfflineHtml())
    }

    @Test
    fun `cleanup preserves a physical image shared by another language until its last catalog owner is removed`() = runBlocking {
        val english = savedPage(ReadingList("Planets", null))
        val french = english.copy(id = 0, lang = "fr")
        french.id = database.readingListPageDao().insertReadingListPage(french)
        val path = File(temporaryFolder.root, "shared-image").path
        val image = File("$path.1").apply { writeText("shared image") }
        File("$path.0").writeText("metadata")
        listOf(english, french).forEach { owner ->
            database.offlineObjectDao().insertOfflineObject(OfflineObject(
                url = "https://upload.wikimedia.org/wikipedia/commons/Saturn.jpg", lang = owner.lang, path = path, status = 0
            ).apply { addUsedBy(owner.id) })
        }

        database.readingListPageDao().markPagesForOffline(listOf(english), false, false)
        removeOfflineObjects()

        assertTrue(image.exists())
        assertEquals(listOf(french.id), database.offlineObjectDao().getAllObjects().single().usedBy)
        assertTrue(database.readingListPageDao().getPageById(french.id)!!.offline)
        removeOfflineObjectsFor(listOf(french.id))
        assertFalse(image.exists())
    }

    @Test
    fun `sync metadata from an older snapshot preserves the current offline request`() = runBlocking {
        val saved = savedPage(ReadingList("Planets", null))
        database.readingListPageDao().markPagesForOffline(listOf(saved), false, false)

        database.readingListPageDao().updateRemoteIds(listOf(saved.copy(remoteId = 16)))
        database.readingListPageDao().updateSyncMetadata(saved.id, 17, 1000, 2000)

        val page = database.readingListPageDao().getPageById(saved.id)!!
        assertFalse(page.offline)
        assertEquals(17L, page.remoteId)
        assertEquals(1000L, page.atime)
        assertEquals(2000L, page.mtime)
    }

    @Test
    fun `opening a stale page touches its collection without restoring offline intent or saved timestamp`() = runBlocking {
        val list = ReadingList("Planets", null, atime = 0)
        val page = savedPage(list).copy(atime = 123)
        val dao = database.readingListPageDao()
        dao.markPagesForOffline(listOf(page), false, false)
        ReadingListBehaviorsUtil.touchListsForPage(page)
        withTimeout(5000) {
            while (database.readingListDao().getListById(list.id)!!.atime == 0L) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(1)
            }
        }
        val latest = dao.getPageById(page.id)!!
        assertFalse(latest.offline)
        assertEquals(0L, latest.atime)
    }

    @Test
    fun `a returned cache response remains readable after its catalog path is replaced`() = runBlocking {
        val page = savedPage(ReadingList("Planets", null))
        val oldFiles = cachedArticle(page)
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor {
            throw IOException("Test network unavailable")
        }.build()
        val url = "https://en.wikipedia.org/api/rest_v1/page/mobile-html/Saturn"
        val request = okhttp3.Request.Builder().url(url).header(OfflineCacheInterceptor.LANG_HEADER, "en").build()

        client.newCall(request).execute().use { first ->
            val staged = File(temporaryFolder.root, "new-html").path
            File("$staged.0").writeText("$url\nGET\nHTTP_2\n200\nOK\nContent-Type: text/html\n")
            File("$staged.1").writeText("<p>New Saturn content.</p>")
            val dao = database.offlineObjectDao()
            OfflineCacheWrite.start(page.articleKey()).use { write ->
                val retired = dao.publishResponse(OfflineObject(url = url, lang = "en", path = staged, status = 0),
                    File(temporaryFolder.root, "published-html").path, write, sharedImage = false)!!
                dao.deleteUnreferencedFiles(retired)
            }
            assertFalse(oldFiles.last().exists())
            assertEquals("<p>Saturn has rings.</p>", first.body.string())
        }
        assertEquals("<p>New Saturn content.</p>", client.newCall(request).execute().use { it.body.string() })
    }

    @Test
    fun `missing cached HTML prevents reuse and membership transfer`() = runBlocking {
        val page = savedPage(ReadingList("Planets", null))
        val files = cachedArticle(page)
        assertTrue(files.last().delete())
        val dao = database.readingListPageDao()

        val visible = dao.findPageInAnyList(title)!!
        val target = ReadingList("Astronomy", null).apply { id = database.readingListDao().insertReadingList(this) }
        dao.addPagesToList(target, listOf(ReadingListPage(title)), false)
        val member = dao.getPageByTitle(target, title)!!
        assertFalse(visible.availableOffline)
        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_SAVE, member.status)
        dao.markPagesForOffline(listOf(page), true, false)
        assertTrue(dao.getAllPageOccurrences(title).all { it.status == ReadingListPage.STATUS_QUEUE_FOR_SAVE })
    }

    @Test
    fun `same URL and physical path from a different language does not complete this article`() = runBlocking {
        val page = savedPage(ReadingList("Planets", null))
        cachedArticle(page)
        val cache = database.offlineObjectDao()
        val html = cache.getAllObjects().single { it.url.contains("/mobile-html/") }
        cache.deleteOfflineObject(html)
        cache.insertOfflineObject(html.copy(id = 0, lang = "fr"))

        val dao = database.readingListPageDao()
        assertFalse(dao.findPageInAnyList(title)!!.availableOffline)
        dao.markPagesForOffline(listOf(page), true, false)
        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_SAVE, dao.getPageById(page.id)!!.status)
        assertTrue(File(html.path + ".1").exists())
    }

    @Test
    fun `WebView closes the response when it cannot use the body`() {
        var closed = false
        val trackedSource = object : ForwardingSource(Buffer().writeUtf8("response")) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = trackedSource
        }
        mockkObject(OkHttpConnectionFactory)
        every { OkHttpConnectionFactory.client } returns OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body).build()
        }.build()
        val client = object : OkHttpWebViewClient() {
            override val model = PageViewModel()
            override val linkHandler = mockk<LinkHandler>()
        }
        val request = mockk<WebResourceRequest> {
            every { url } returns Uri.parse("https://en.wikipedia.org/resource.css")
            every { method } returns "GET"
            every { requestHeaders } returns emptyMap()
        }

        assertEquals(500, client.shouldInterceptRequest(mockk<WebView>(), request)!!.statusCode)
        assertTrue(closed)
    }

    private suspend fun cachedArticle(page: ReadingListPage): List<File> {
        page.sizeBytes = 100
        database.readingListPageDao().updateReadingListPage(page)
        return listOf("summary", "mobile-html").map { endpoint ->
            val url = "https://en.wikipedia.org/api/rest_v1/page/$endpoint/Saturn"
            val path = File(temporaryFolder.root, endpoint).path
            File("$path.0").writeText("$url\nGET\nHTTP_2\n200\nOK\nContent-Type: text/html\n")
            val content = File("$path.1").apply { writeText("<p>Saturn has rings.</p>") }
            database.offlineObjectDao().insertOfflineObject(OfflineObject(url = url, lang = "en", path = path, status = 0)
                .apply { addUsedBy(page.id) })
            content
        }
    }

    private fun readOfflineHtml(): String {
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor {
            throw java.io.IOException("Test network unavailable")
        }.build()
        return client.newCall(okhttp3.Request.Builder().url("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Saturn")
            .header(OfflineCacheInterceptor.LANG_HEADER, "en").build()).execute().use { it.body.string() }
    }

    private suspend fun requestedSaveHeader(): String? {
        val pageModel = PageViewModel().apply {
            title = this@ReadingListOfflineTest.title
            readingListPage = database.readingListPageDao().findPageInAnyList(this@ReadingListOfflineTest.title)
        }
        var saveHeader: String? = "request not sent"
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            saveHeader = chain.request().header(OfflineCacheInterceptor.SAVE_HEADER)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("body".toResponseBody("text/css".toMediaType())).build()
        }.build()
        mockkObject(OkHttpConnectionFactory)
        every { OkHttpConnectionFactory.client } returns client
        val webClient = object : OkHttpWebViewClient() {
            override val model = pageModel
            override val linkHandler = mockk<LinkHandler>()
        }
        val request = mockk<WebResourceRequest> {
            every { url } returns Uri.parse("https://en.wikipedia.org/resource.css")
            every { method } returns "GET"
            every { requestHeaders } returns emptyMap()
        }
        webClient.shouldInterceptRequest(mockk<WebView>(), request)?.data?.close()
        return saveHeader
    }

    private suspend fun savedPage(list: ReadingList): ReadingListPage {
        list.id = database.readingListDao().insertReadingList(list)
        val page = ReadingListPage(WikiSite.forLanguageCode("en"), Namespace.MAIN, "Saturn", "Saturn",
            listId = list.id, offline = true, status = ReadingListPage.STATUS_SAVED)
        page.id = database.readingListPageDao().insertReadingListPage(page)
        return page
    }

    private suspend fun cachedObject(vararg pages: ReadingListPage): File {
        val path = File(temporaryFolder.root, "saturn").path
        File("$path.0").writeText("metadata")
        val contents = File("$path.1").apply { writeText("article contents") }
        val obj = OfflineObject(url = "https://en.wikipedia.org/Saturn", lang = "en", path = path, status = 0)
        pages.forEach { obj.addUsedBy(it.id) }
        database.offlineObjectDao().insertOfflineObject(obj)
        return contents
    }

    private suspend fun removeOfflineObjectsFor(ids: List<Long>) {
        val dao = database.offlineObjectDao()
        dao.deleteUnreferencedFiles(dao.detachObjectsFromPages(ids))
    }

    private suspend fun removeOfflineObjects() {
        val pages = database.readingListPageDao().getAllPagesToBeUnsaved()
        removeOfflineObjectsFor(pages.map { it.id })
    }
}
