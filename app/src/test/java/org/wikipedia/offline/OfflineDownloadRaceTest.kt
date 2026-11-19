package org.wikipedia.offline

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.room.Room
import androidx.work.WorkerParameters
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.json.MwExceptionConverterFactory
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.readinglist.ArticleOfflineStateStore
import org.wikipedia.readinglist.articleKey
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.savedpages.SavedPageSyncNotification
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class OfflineDownloadRaceTest {
    private lateinit var database: AppDatabase
    private val title get() = PageTitle("Saturn", WikiSite.forLanguageCode("en"))
    private val cacheDirectory get() = File(WikipediaApp.instance.filesDir, OfflineCacheInterceptor.OFFLINE_PATH)
    private val stagingDirectory get() = File(WikipediaApp.instance.filesDir, "offline-responses")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        mockkObject(AppDatabase.Companion, SavedPageSyncService.Companion, SavedPageSyncNotification.instance,
            ReadingListSyncAdapter.Companion, ServiceFactory, OkHttpConnectionFactory, Prefs)
        every { AppDatabase.instance } returns database
        every { SavedPageSyncService.enqueue() } just Runs
        every { ReadingListSyncAdapter.inProgress() } returns false
        every { SavedPageSyncNotification.instance.setNotificationProgress(any(), any(), any()) } just Runs
        every { SavedPageSyncNotification.instance.cancelNotification(any()) } just Runs
        every { Prefs.isDownloadOnlyOverWiFiEnabled } returns false
        every { Prefs.isDownloadingReadingListArticlesEnabled } returns true
        every { Prefs.isImageDownloadEnabled } returns false
    }

    @After
    fun tearDown() {
        database.close()
        unmockkAll()
    }

    @Test
    fun `OFF while HTML is in flight cannot restore the offline request`() = runBlocking {
        val page = queuedPage()
        duringDownload { database.readingListPageDao().markPagesForOffline(listOf(page), false, false) }

        assertFalse(database.readingListPageDao().getPageById(page.id)!!.offline)
        assertTrue(database.offlineObjectDao().getAllObjects().none { it.url.endsWith("/mobile-html/Saturn") })
    }

    @Test
    fun `deleting a membership during download cannot restore the deleted row`() = runBlocking {
        val page = queuedPage()
        val list = database.readingListDao().getListById(page.listId)!!
        duringDownload { database.readingListPageDao().markPagesForDeletion(list, listOf(page), false) }

        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_DELETE, database.readingListPageDao().getPageById(page.id)!!.status)
        assertTrue(database.readingListPageDao().getAllPageOccurrences(title).isEmpty())
    }

    @Test
    fun `a rejected image response leaves another language's shared path intact`() = runBlocking {
        val page = queuedPage()
        val french = queuedPage("fr")
        val image = "https://upload.wikimedia.org/wikipedia/commons/Saturn.jpg"
        cacheDirectory.mkdirs()
        val path = File(cacheDirectory, StringUtil.md5string(image)).path
        File("$path.0").writeText("original metadata")
        File("$path.1").writeText("original image")
        database.offlineObjectDao().insertOfflineObject(OfflineObject(url = image, lang = "fr", path = path, status = 0)
            .apply { addUsedBy(french.id) })
        val model = PageViewModel().apply {
            title = this@OfflineDownloadRaceTest.title
            readingListPage = page
        }
        val client = object : OkHttpWebViewClient() {
            override val model = model
            override val linkHandler = mockk<LinkHandler>()
        }
        every { OkHttpConnectionFactory.client } returns OkHttpClient.Builder()
            .addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
                response(chain, "image/jpeg", "replacement image")
            }.build()
        val request = mockk<WebResourceRequest> {
            every { url } returns Uri.parse(image)
            every { method } returns "GET"
            every { requestHeaders } returns emptyMap()
        }

        val body = client.shouldInterceptRequest(mockk<WebView>(), request)!!.data.bufferedReader()
        body.use {
            assertEquals('r'.code, it.read())
            database.readingListPageDao().markPagesForOffline(listOf(page), false, false)
            assertEquals("eplacement image", it.readText())
        }
        assertEquals("original image", File("$path.1").readText())
        assertEquals(listOf("fr"), database.offlineObjectDao().getAllObjects().map { it.lang })
        assertEquals(2, cacheDirectory.listFiles()!!.size)
        assertTrue(stagingDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `OFF then ON cannot finish a previous download attempt`() = runBlocking {
        val page = queuedPage()
        val dao = database.readingListPageDao()
        duringDownload {
            dao.markPagesForOffline(listOf(page), false, false)
            dao.markPagesForOffline(listOf(page), true, false)
        }

        val pending = dao.getPageById(page.id)!!
        assertTrue(pending.offline)
        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_SAVE, pending.status)
        assertTrue(database.offlineObjectDao().getAllObjects().none { it.url.endsWith("/mobile-html/Saturn") })

        duringDownload {}
        assertTrue(ArticleOfflineStateStore(database).snapshot().getValue(page.articleKey()).downloaded)
    }

    @Test
    fun `one worker download completes current memberships and preserves concurrent sync metadata`() = runBlocking {
        val first = queuedPage()
        val second = queuedPage()
        val requests = duringDownload {
            database.readingListPageDao().updateSyncMetadata(second.id, 88, 7, 9)
        }

        assertEquals(1, requests)
        val current = database.readingListPageDao().getAllPageOccurrences(title)
        assertEquals(setOf(first.id, second.id), current.mapTo(mutableSetOf()) { it.id })
        assertTrue(current.all { it.offline && it.status == ReadingListPage.STATUS_SAVED && it.sizeBytes > 0 })
        assertEquals(88L, current.first { it.id == second.id }.remoteId)
        assertTrue(database.offlineObjectDao().getAllObjects().all { it.usedBy.toSet() == setOf(first.id, second.id) })
        assertTrue(ArticleOfflineStateStore(database).snapshot().getValue(first.articleKey()).downloaded)
        val offlineClient = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor {
            throw IOException("Test network unavailable")
        }.build()
        val html = Request.Builder().url("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Saturn")
            .header(OfflineCacheInterceptor.LANG_HEADER, "en").build()
        assertEquals("<p>Saturn has rings.</p>", offlineClient.newCall(html).execute().use { it.body.string() })
    }

    @Test
    fun `worker verifies a summary title containing namespace punctuation`() = runBlocking {
        val page = queuedPage(apiTitle = "Talk:Saturn", namespace = Namespace.TALK)
        duringDownload {}
        assertTrue(ArticleOfflineStateStore(database).snapshot().getValue(page.articleKey()).downloaded)
    }

    @Test
    fun `persistent staging reclaims abandoned files and keeps a concurrent response intact`() = runBlocking {
        val page = queuedPage()
        stagingDirectory.mkdirs()
        val abandoned = listOf("response-abandoned.0", "response-abandoned.1").map {
            File(stagingDirectory, it).apply { writeText("abandoned response") }
        }
        cacheDirectory.mkdirs()
        val existing = File(cacheDirectory, "published.1").apply { writeText("existing copy") }
        fun client() = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
            response(chain, "text/css", "body { color: black; }")
        }.build()
        val request = Request.Builder().url("https://en.wikipedia.org/resource.css")
            .header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .header(OfflineCacheInterceptor.LANG_HEADER, "en")
            .header(OfflineCacheInterceptor.TITLE_HEADER, "Saturn")
            .tag(ArticleCacheRequest::class.java, ArticleCacheRequest(page.articleKey())).build()

        client().newCall(request).execute().use {
            assertTrue(abandoned.none { it.exists() })
            val active = stagingDirectory.listFiles().orEmpty()
            assertEquals(2, active.size)

            client().newCall(request).execute().use { response -> assertEquals("body { color: black; }", response.body.string()) }

            assertTrue(active.all { it.isFile })
            val published = database.offlineObjectDao().getAllObjects().single()
            assertEquals(cacheDirectory, File(published.path).parentFile)
            assertEquals("existing copy", existing.readText())
        }
        assertTrue(stagingDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `closing an incomplete response removes staged files and leaves the catalog empty`() = runBlocking {
        val page = queuedPage()
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
            response(chain, "text/css", "a".repeat(16384))
        }.build()
        val request = Request.Builder().url("https://en.wikipedia.org/resource.css")
            .header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .header(OfflineCacheInterceptor.LANG_HEADER, "en")
            .header(OfflineCacheInterceptor.TITLE_HEADER, "Saturn")
            .tag(ArticleCacheRequest::class.java, ArticleCacheRequest(page.articleKey())).build()

        client.newCall(request).execute().use { assertEquals('a'.code, it.body.byteStream().read()) }

        assertTrue(database.offlineObjectDao().getAllObjects().isEmpty())
        assertTrue(cacheDirectory.listFiles().orEmpty().isEmpty())
        assertTrue(stagingDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `untagged article response cannot create an ownerless catalog entry`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
            response(chain, "text/html", "<p>Saturn has rings.</p>")
        }.build()
        val request = Request.Builder().url("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Saturn")
            .header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .header(OfflineCacheInterceptor.LANG_HEADER, "en")
            .header(OfflineCacheInterceptor.TITLE_HEADER, "Saturn").build()
        client.newCall(request).execute().use { it.body.string() }

        assertTrue(database.offlineObjectDao().getAllObjects().isEmpty())
        assertTrue(cacheDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `Talk responses are still cached without a reading list membership`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
            response(chain, "application/json", """{"discussiontoolspageinfo":{}}""")
        }.build()
        val request = Request.Builder().url("https://en.wikipedia.org/w/api.php?action=discussiontoolspageinfo")
            .header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .header(OfflineCacheInterceptor.LANG_HEADER, "en")
            .header(OfflineCacheInterceptor.TITLE_HEADER, "Talk:Saturn").build()
        client.newCall(request).execute().use { it.body.string() }
        val record = database.offlineObjectDao().getAllObjects().single()
        assertTrue(record.usedBy.isEmpty())
        assertTrue(File(record.path + ".1").isFile)
    }

    @Test
    fun `cancelled worker cannot publish the late response while request remains enabled`() = runBlocking {
        val page = queuedPage()
        duringDownload(cancelWorker = true) {}

        val pending = database.readingListPageDao().getPageById(page.id)!!
        assertTrue(pending.offline)
        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_SAVE, pending.status)
        assertTrue(database.offlineObjectDao().getAllObjects().none { it.url.endsWith("/mobile-html/Saturn") })
        assertTrue(stagingDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `empty HTML does not mark article downloaded even with positive summary bytes`() = runBlocking {
        val page = queuedPage()
        duringDownload(htmlBody = "") {}

        val pending = database.readingListPageDao().getPageById(page.id)!!
        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_SAVE, pending.status)
        assertFalse(ArticleOfflineStateStore(database).snapshot().getValue(page.articleKey()).downloaded)
    }

    @Test
    fun `deleting the original row still allows completion for the remaining requested membership`() = runBlocking {
        val first = queuedPage()
        val second = queuedPage()
        val list = database.readingListDao().getListById(first.listId)!!
        duringDownload {
            database.readingListPageDao().markPagesForDeletion(list, listOf(first), false)
        }

        assertEquals(ReadingListPage.STATUS_QUEUE_FOR_DELETE, database.readingListPageDao().getPageById(first.id)!!.status)
        val remaining = database.readingListPageDao().getAllPageOccurrences(title).single()
        assertEquals(second.id, remaining.id)
        assertEquals(ReadingListPage.STATUS_SAVED, remaining.status)
        assertTrue(ArticleOfflineStateStore(database).snapshot().getValue(second.articleKey()).downloaded)
    }

    @Test
    fun `permanent download error settles unavailable until the user explicitly retries`() = runBlocking {
        val page = queuedPage()
        val dao = database.readingListPageDao()
        duringDownload(htmlCode = 404) {}
        val settled = dao.findPageInAnyList(title)!!
        assertTrue(settled.offlineRequested)
        assertFalse(settled.availableOffline)
        assertFalse(settled.saving)
        assertEquals(ReadingListPage.STATUS_DOWNLOAD_FAILED, settled.status)
        assertTrue(dao.getAllPagesToBeSaved().isEmpty())
        val list = ReadingList("Other planets", null).apply { id = database.readingListDao().insertReadingList(this) }
        dao.addPagesToList(list, listOf(ReadingListPage(title)), false)
        assertTrue(dao.getAllPagesToBeSaved().isEmpty())
        assertTrue(dao.getAllPageOccurrences(title).all { it.status == ReadingListPage.STATUS_DOWNLOAD_FAILED })

        dao.markPagesForOffline(listOf(settled), true, false)
        assertTrue(dao.findPageInAnyList(title)!!.saving)
        duringDownload {}
        assertTrue(dao.findPageInAnyList(title)!!.availableOffline)
    }

    private suspend fun queuedPage(lang: String = "en", apiTitle: String = "Saturn", namespace: Namespace = Namespace.MAIN): ReadingListPage {
        val listId = database.readingListDao().insertReadingList(ReadingList("Planets", null))
        return ReadingListPage(title).copy(listId = listId, lang = lang, apiTitle = apiTitle, namespace = namespace, offline = true,
            status = ReadingListPage.STATUS_QUEUE_FOR_SAVE).apply {
            id = database.readingListPageDao().insertReadingListPage(this)
        }
    }

    private suspend fun duringDownload(cancelWorker: Boolean = false, htmlBody: String = "<p>Saturn has rings.</p>", htmlCode: Int = 200,
                                       change: suspend () -> Unit): Int = coroutineScope {
        val started = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val htmlRequests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor(OfflineCacheInterceptor()).addInterceptor { chain ->
            when {
                chain.request().url.encodedPath.contains("/mobile-html/") -> {
                    htmlRequests.incrementAndGet()
                    started.countDown()
                    check(resume.await(5, TimeUnit.SECONDS)) { "Test did not release HTML response" }
                    response(chain, "text/html", htmlBody, htmlCode)
                }
                chain.request().url.encodedPath.contains("/summary/") ->
                    response(chain, "application/json", """{"titles":{"canonical":"Saturn","display":"Saturn"}}""")
                else -> response(chain, "application/json", """{"items":[]}""")
            }
        }.build()
        every { OkHttpConnectionFactory.client } returns client
        every { ServiceFactory.getRest(any()) } returns Retrofit.Builder()
            .baseUrl(ServiceFactory.getRestBasePath(title.wikiSite))
            .client(client).addConverterFactory(MwExceptionConverterFactory()).build().create(RestService::class.java)
        val work = async(Dispatchers.IO) { SavedPageSyncService(RuntimeEnvironment.getApplication(), mockk<WorkerParameters>(relaxed = true)).doWork() }
        try {
            assertTrue("Worker did not request HTML", withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS) })
            change()
            if (cancelWorker) work.cancel()
        } finally {
            resume.countDown()
        }
        if (cancelWorker) work.join() else work.await()
        htmlRequests.get()
    }

    private fun response(chain: Interceptor.Chain, mimeType: String, body: String, code: Int = 200): Response {
        val network = Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Test response").build()
        return network.newBuilder().networkResponse(network).header("Content-Type", mimeType)
            .body(body.toResponseBody(mimeType.toMediaType())).build()
    }
}
