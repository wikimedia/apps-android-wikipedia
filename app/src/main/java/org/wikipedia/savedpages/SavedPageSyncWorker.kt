package org.wikipedia.savedpages

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.Buffer
import okio.Sink
import okio.Timeout
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.gallery.MediaList
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class SavedPageSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val savedPageSyncNotification = SavedPageSyncNotification.instance
    private val app = appContext as WikipediaApp

    override suspend fun doWork(): Result {
        if (ReadingListSyncAdapter.inProgress()) {
            // Reading list sync was started in the meantime, so bail.
            return Result.failure()
        }
        val pagesToSave = withContext(Dispatchers.IO) {
            AppDatabase.getAppDatabase().readingListPageDao().getAllPagesToBeForcedSave().toMutableList()
        }
        if ((!Prefs.isDownloadOnlyOverWiFiEnabled || DeviceUtil.isOnWiFi) && Prefs.isDownloadingReadingListArticlesEnabled) {
            val pagesToBeSaved = withContext(Dispatchers.IO) {
                AppDatabase.getAppDatabase().readingListPageDao().getAllPagesToBeSaved()
            }
            pagesToSave.addAll(pagesToBeSaved)
        }
        val pagesToUnSave = withContext(Dispatchers.IO) {
            AppDatabase.getAppDatabase().readingListPageDao().getAllPagesToBeUnsaved()
        }
        val pagesToDelete = withContext(Dispatchers.IO) {
            AppDatabase.getAppDatabase().readingListPageDao().getAllPagesToBeDeleted()
        }
        var shouldSendSyncEvent = false
        try {
            for (page in pagesToDelete) {
                deletePageContents(page)
            }
            for (page in pagesToUnSave) {
                deletePageContents(page)
            }
        } catch (e: Exception) {
            L.e("Error while deleting page: " + e.message)
        } finally {
            if (pagesToDelete.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    AppDatabase.getAppDatabase().readingListPageDao().purgeDeletedPages()
                }
                shouldSendSyncEvent = true
            }
            if (pagesToUnSave.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    AppDatabase.getAppDatabase().readingListPageDao().resetUnsavedPageStatus()
                }
                shouldSendSyncEvent = true
            }
        }
        val itemsTotal = pagesToSave.size
        if (itemsTotal > 0) {
            shouldSendSyncEvent = true
        }
        var itemsSaved = 0
        try {
            savePages(pagesToSave)
        } finally {
            if (savedPageSyncNotification.isSyncPaused()) {
                savedPageSyncNotification.setNotificationPaused(applicationContext, itemsTotal, itemsSaved)
            } else {
                savedPageSyncNotification.cancelNotification(applicationContext)
                savedPageSyncNotification.setSyncCanceled(false)
                if (shouldSendSyncEvent) {
                    sendSyncEvent()
                }
            }
        }.also { itemsSaved = it }
        return Result.success()
    }

    private suspend fun deletePageContents(page: ReadingListPage) {
        try {
            withContext(Dispatchers.IO) {
                AppDatabase.getAppDatabase().offlineObjectDao().deleteObjectsForPageId(page.id)
            }
        } catch (t: Throwable) {
            L.e(t)
        }
    }

    private suspend fun savePages(queue: MutableList<ReadingListPage>): Int {
        val itemsTotal = queue.size
        var itemsSaved = 0
        while (queue.isNotEmpty()) {
            // Pick off the DB row that we'll be working on...
            val page = queue.removeAt(0)
            if (savedPageSyncNotification.isSyncPaused()) {
                // Remaining transactions will be picked up again when the service is resumed.
                break
            } else if (savedPageSyncNotification.isSyncCanceled()) {
                // Mark remaining pages as online-only!
                queue.add(page)
                withContext(Dispatchers.IO) {
                    AppDatabase.getAppDatabase().readingListPageDao()
                        .markPagesForOffline(queue, offline = false, forcedSave = false)
                }
                break
            }
            savedPageSyncNotification.setNotificationProgress(applicationContext, itemsTotal, itemsSaved)
            var success = false
            var totalSize = 0L
            try {
                // Lengthy operation during which the db state may change...
                totalSize = withContext(Dispatchers.IO) { savePageFor(page) }
                success = true
            } catch (e: InterruptedException) {
                // fall through
            } catch (e: Exception) {
                // This can be an IOException from the storage media, or several types
                // of network exceptions from malformed URLs, timeouts, etc.
                L.e(e)

                // If we're offline, or if there's a transient network error, then don't do
                // anything.  Otherwise...
                if (!ThrowableUtil.isOffline(e) && !ThrowableUtil.isTimeout(e) && !ThrowableUtil.isNetworkError(e)) {
                    // If it's not a transient network error (e.g. a 404 status response), it implies
                    // that there's no way to fetch the page next time, or ever, therefore let's mark
                    // it as "successful" so that it won't be retried again.
                    success = true
                    if (e !is HttpStatusException) {
                        // And if it's something other than an HTTP status, let's log it and see what it is.
                        L.logRemoteError(e)
                    }
                }
            }
            if (ReadingListSyncAdapter.inProgress()) {
                // Reading list sync was started in the meantime, so bail.
                break
            }
            if (success) {
                page.status = ReadingListPage.STATUS_SAVED
                page.sizeBytes = totalSize
                withContext(Dispatchers.IO) {
                    AppDatabase.getAppDatabase().readingListPageDao().updateReadingListPage(page)
                }
                itemsSaved++
                sendSyncEvent()
            }
        }
        return itemsSaved
    }

    @Throws(Exception::class)
    private suspend fun savePageFor(page: ReadingListPage): Long {
        val pageTitle = ReadingListPage.toPageTitle(page)

        try {
            val summaryRsp = reqPageSummary(pageTitle)
            val revision = summaryRsp.body()?.revision ?: 0
            val mediaListRsp = reqMediaList(pageTitle, revision)
            val mobileHTMLRsp = reqMobileHTML(pageTitle)

            page.downloadProgress = SUMMARY_PROGRESS
            app.bus.post(PageDownloadEvent(page))
            page.downloadProgress = MOBILE_HTML_SECTION_PROGRESS
            app.bus.post(PageDownloadEvent(page))
            page.downloadProgress = MEDIA_LIST_PROGRESS
            app.bus.post(PageDownloadEvent(page))
            val fileUrls = mutableSetOf<String>()

            // download css and javascript assets
            mobileHTMLRsp.body?.let {
                fileUrls.addAll(
                    PageComponentsUrlParser.parse(it.string(), pageTitle.wikiSite).filter { url -> url.isNotEmpty() }
                )
            }
            if (Prefs.isImageDownloadEnabled) {
                // download thumbnail and lead image
                val thumbnailUrl = summaryRsp.body()!!.thumbnailUrl
                if (!thumbnailUrl.isNullOrEmpty()) {
                    page.thumbUrl = UriUtil.resolveProtocolRelativeUrl(pageTitle.wikiSite, thumbnailUrl)
                    AppDatabase.getAppDatabase().pageImagesDao().insertPageImage(PageImage(pageTitle, page.thumbUrl!!))
                    fileUrls.add(
                        UriUtil.resolveProtocolRelativeUrl(
                            ImageUrlUtil.getUrlForPreferredSize(
                                page.thumbUrl!!,
                                DimenUtil.calculateLeadImageWidth()
                            )
                        )
                    )
                }

                // download article images
                for (item in mediaListRsp.body()!!.getItems("image")) {
                    item.srcSets.let {
                        fileUrls.add(item.getImageUrl(DimenUtil.densityScalar))
                    }
                }
            }
            page.displayTitle = summaryRsp.body()!!.displayTitle
            page.description = summaryRsp.body()!!.description
            reqSaveFiles(page, pageTitle, fileUrls)
            val totalSize = AppDatabase.getAppDatabase().offlineObjectDao().getTotalBytesForPageId(page.id)
            L.i("Saved page " + pageTitle.prefixedText + " ($totalSize)")
            return totalSize
        } catch (t: Throwable) {
            throw t
        }
    }

    private suspend fun reqPageSummary(pageTitle: PageTitle): Response<PageSummary> {
        return ServiceFactory.getRest(pageTitle.wikiSite).getSummaryResponseSuspend(pageTitle.prefixedText,
            null, OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
            OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode,
            UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private suspend fun reqMediaList(pageTitle: PageTitle, revision: Long): Response<MediaList> {
        return ServiceFactory.getRest(pageTitle.wikiSite).getMediaListResponse(pageTitle.prefixedText,
            revision, OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
            OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode,
            UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private suspend fun reqMobileHTML(pageTitle: PageTitle) = withContext(Dispatchers.IO) {
        val request = makeUrlRequest(pageTitle.wikiSite,
            ServiceFactory.getRestBasePath(pageTitle.wikiSite) +
                    RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText),
            pageTitle).build()
        OkHttpConnectionFactory.client.newCall(request).execute()
    }

    @Throws(IOException::class, InterruptedException::class)
    private suspend fun reqSaveFiles(page: ReadingListPage, pageTitle: PageTitle, urls: Set<String>) {
        val numOfImages = urls.size
        var percentage = MEDIA_LIST_PROGRESS.toFloat()
        val updateRate = (CircularProgressBar.MAX_PROGRESS - percentage) / numOfImages
        for (url in urls) {
            if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw InterruptedException("Sync paused or cancelled.")
            }
            try {
                reqSaveUrl(pageTitle, page.wiki, url)
                percentage += updateRate
                page.downloadProgress = percentage.toInt()
                app.bus.post(PageDownloadEvent(page))
            } catch (e: Exception) {
                if (isRetryable(e)) {
                    throw e
                }
            }
        }
        page.downloadProgress = CircularProgressBar.MAX_PROGRESS
        app.bus.post(PageDownloadEvent(page))
    }

    @Throws(IOException::class)
    private suspend fun reqSaveUrl(pageTitle: PageTitle, wiki: WikiSite, url: String) = withContext(Dispatchers.IO) {
        val request = makeUrlRequest(wiki, url, pageTitle).build()
        val rsp = OkHttpConnectionFactory.client.newCall(request).execute()

        // Read the entirety of the response, so that it's written to cache by the interceptor.
        rsp.body!!.source().readAll(object : Sink {
            override fun write(source: Buffer, byteCount: Long) {}
            override fun flush() {}
            override fun timeout(): Timeout {
                return Timeout()
            }

            override fun close() {}
        })
        rsp.body!!.close()
    }

    private fun makeUrlRequest(wiki: WikiSite, url: String, pageTitle: PageTitle): Request.Builder {
        return Request.Builder().cacheControl(OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK).url(
            UriUtil.resolveProtocolRelativeUrl(wiki, url))
            .addHeader("Accept-Language", app.getAcceptLanguage(pageTitle.wikiSite))
            .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .addHeader(OfflineCacheInterceptor.LANG_HEADER, pageTitle.wikiSite.languageCode)
            .addHeader(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private fun isRetryable(t: Throwable): Boolean {
        // "Retryable" in this case refers to exceptions that will be rethrown up to the
        // outer exception handler, so that the entire page can be retried on the next pass
        // of the sync service.

        // Errors that do *not* qualify for retrying include:
        // - IllegalArgumentException (thrown for any kind of malformed URL)
        // - HTTP 404 status (for nonexistent media)
        return !(t is IllegalArgumentException || ThrowableUtil.is404(t))
    }

    companion object {
        private const val WORK_NAME = "saved_page_sync"
        const val SUMMARY_PROGRESS = 10
        const val MOBILE_HTML_SECTION_PROGRESS = 20
        const val MEDIA_LIST_PROGRESS = 30

        @JvmStatic
        fun enqueue() {
            if (ReadingListSyncAdapter.inProgress()) {
                return
            }
            val workRequest = OneTimeWorkRequestBuilder<SavedPageSyncWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(WikipediaApp.getInstance())
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        @JvmStatic
        fun sendSyncEvent(showMessage: Boolean = false) {
            // Note: this method posts from a background thread but subscribers expect events to be
            // received on the main thread.
            WikipediaApp.getInstance().bus.post(ReadingListSyncEvent(showMessage))
        }
    }
}
