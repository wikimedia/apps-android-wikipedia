package org.wikipedia.savedpages

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.Buffer
import okio.Sink
import okio.Timeout
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar
import java.io.IOException
import java.util.concurrent.TimeUnit

class SavedPageSyncService(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val savedPageSyncNotification = SavedPageSyncNotification.instance

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            if (ReadingListSyncAdapter.inProgress()) {
                // Reading list sync was started in the meantime, so bail.
                return@withContext Result.success()
            }
            val pagesToSave = AppDatabase.instance.readingListPageDao().getAllPagesToBeForcedSave().toMutableList()
            if ((!Prefs.isDownloadOnlyOverWiFiEnabled || DeviceUtil.isOnWiFi) &&
                Prefs.isDownloadingReadingListArticlesEnabled) {
                pagesToSave.addAll(AppDatabase.instance.readingListPageDao().getAllPagesToBeSaved())
            }
            val pagesToUnSave = AppDatabase.instance.readingListPageDao().getAllPagesToBeUnsaved()
            val pagesToDelete = AppDatabase.instance.readingListPageDao().getAllPagesToBeDeleted()
            var shouldSendSyncEvent = false
            try {
                AppDatabase.instance.offlineObjectDao().deleteObjectsForPageId(pagesToDelete.map { it.id })
                AppDatabase.instance.offlineObjectDao().deleteObjectsForPageId(pagesToUnSave.map { it.id })
            } catch (e: Exception) {
                L.e("Error while deleting page: " + e.message)
            } finally {
                if (pagesToDelete.isNotEmpty()) {
                    AppDatabase.instance.readingListPageDao().purgeDeletedPages()
                    shouldSendSyncEvent = true
                }
                if (pagesToUnSave.isNotEmpty()) {
                    AppDatabase.instance.readingListPageDao().updateStatus(
                        oldStatus = ReadingListPage.STATUS_SAVED,
                        newStatus = ReadingListPage.STATUS_QUEUE_FOR_SAVE,
                        offline = false
                    )
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
            Result.success()
        }
    }

    private suspend fun savePages(queue: MutableList<ReadingListPage>): Int {
        return withContext(Dispatchers.IO) {
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
                    AppDatabase.instance.readingListPageDao().markPagesForOffline(queue, offline = false, forcedSave = false)
                    break
                }
                savedPageSyncNotification.setNotificationProgress(applicationContext, itemsTotal, itemsSaved)
                var success = false
                var totalSize = 0L
                try {
                    // Lengthy operation during which the db state may change...
                    totalSize = savePageFor(page)
                    success = true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: InterruptedException) {
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
                    AppDatabase.instance.readingListPageDao().updateReadingListPage(page)
                    itemsSaved++
                    sendSyncEvent()
                }
            }
            itemsSaved
        }
    }

    @Throws(Exception::class)
    private suspend fun savePageFor(page: ReadingListPage): Long {
        return withContext(Dispatchers.IO) {
            val pageTitle = ReadingListPage.toPageTitle(page)

            val summaryCall = async { ServiceFactory.getRest(pageTitle.wikiSite).getPageSummary(pageTitle.prefixedText,
                cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
                saveHeader = OfflineCacheInterceptor.SAVE_HEADER_SAVE, langHeader = pageTitle.wikiSite.languageCode,
                titleHeader = UriUtil.encodeURL(pageTitle.prefixedText)) }

            val mediaListCall = async { ServiceFactory.getRest(pageTitle.wikiSite).getMediaList(pageTitle.prefixedText,
                cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
                saveHeader = OfflineCacheInterceptor.SAVE_HEADER_SAVE, langHeader = pageTitle.wikiSite.languageCode,
                titleHeader = UriUtil.encodeURL(pageTitle.prefixedText)) }

            val mobileHTMLCall = async { reqMobileHTML(pageTitle) }

            val summaryResponse = summaryCall.await()
            val mediaListResponse = mediaListCall.await()
            val mobileHTMLResponse = mobileHTMLCall.await()

            page.downloadProgress = MEDIA_LIST_PROGRESS
            FlowEventBus.post(PageDownloadEvent(page))

            val fileUrls = mutableSetOf<String>()
            // download css and javascript assets
            mobileHTMLResponse.body.use {
                fileUrls.addAll(PageComponentsUrlParser.parse(it.string(),
                    pageTitle.wikiSite).filter { url -> url.isNotEmpty() })
            }
            if (Prefs.isImageDownloadEnabled) {
                // download thumbnail and lead image
                if (!summaryResponse.thumbnailUrl.isNullOrEmpty()) {
                    page.thumbUrl = UriUtil.resolveProtocolRelativeUrl(pageTitle.wikiSite, summaryResponse.thumbnailUrl.orEmpty())
                    val existingPageImage = AppDatabase.instance.pageImagesDao()
                        .findItemsBy(pageTitle.wikiSite.languageCode, pageTitle.namespace, pageTitle.prefixedText)
                        .firstOrNull()

                    AppDatabase.instance.pageImagesDao().insertPageImage(PageImage(
                        pageTitle.wikiSite.languageCode,
                        pageTitle.namespace,
                        page.apiTitle,
                        page.thumbUrl.orEmpty(),
                        summaryResponse.description,
                        existingPageImage?.timeSpentSec ?: 0,
                        summaryResponse.coordinates?.latitude ?: 0.0,
                        summaryResponse.coordinates?.longitude ?: 0.0
                    ))
                    fileUrls.add(UriUtil.resolveProtocolRelativeUrl(
                        ImageUrlUtil.getUrlForPreferredSize(page.thumbUrl.orEmpty(), Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
                }

                // download article images
                for (item in mediaListResponse.getItems("image")) {
                    item.srcSets.let {
                        fileUrls.add(item.getImageUrl(DimenUtil.densityScalar))
                    }
                }
            }
            page.displayTitle = summaryResponse.displayTitle.orEmpty()
            page.description = summaryResponse.description.orEmpty()

            reqSaveFiles(page, pageTitle, fileUrls)

            val totalSize = AppDatabase.instance.offlineObjectDao().getTotalBytesForPageId(page.id)

            L.i("Saved page " + pageTitle.prefixedText + " (" + totalSize + ")")

            totalSize
        }
    }

    private suspend fun reqMobileHTML(pageTitle: PageTitle): okhttp3.Response {
        val request = makeUrlRequest(pageTitle.wikiSite,
                ServiceFactory.getRestBasePath(pageTitle.wikiSite) +
                        RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText),
                pageTitle).build()
        return withContext(Dispatchers.IO) {
            OkHttpConnectionFactory.client.newCall(request).execute()
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private suspend fun reqSaveFiles(page: ReadingListPage, pageTitle: PageTitle, urls: Set<String>) {
        val numOfImages = urls.size
        var percentage = MEDIA_LIST_PROGRESS.toFloat()
        val updateRate = (CircularProgressBar.MAX_PROGRESS - percentage) / numOfImages

        withContext(Dispatchers.IO) {
            for (url in urls) {
                if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                    throw InterruptedException("Sync paused or cancelled.")
                }
                try {
                    reqSaveUrl(pageTitle, page.wiki, url)
                    percentage += updateRate
                    page.downloadProgress = percentage.toInt()
                    FlowEventBus.post(PageDownloadEvent(page))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isRetryable(e)) {
                        throw e
                    }
                }
            }
            page.downloadProgress = CircularProgressBar.MAX_PROGRESS
            FlowEventBus.post(PageDownloadEvent(page))
        }
    }

    @Throws(IOException::class)
    private suspend fun reqSaveUrl(pageTitle: PageTitle, wiki: WikiSite, url: String) {
        val request = makeUrlRequest(wiki, url, pageTitle).build()
        withContext(Dispatchers.IO) {
            OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                // Read the entirety of the response, so that it's written to cache by the interceptor.
                response.body.source().readAll(object : Sink {
                    override fun write(source: Buffer, byteCount: Long) {}
                    override fun flush() {}
                    override fun timeout(): Timeout {
                        return Timeout()
                    }

                    override fun close() {}
                })
            }
        }
    }

    private fun makeUrlRequest(wiki: WikiSite, url: String, pageTitle: PageTitle): Request.Builder {
        return Request.Builder().cacheControl(OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK).url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
                .addHeader("Accept-Language", WikipediaApp.instance.getAcceptLanguage(pageTitle.wikiSite))
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
        private const val WORK_NAME = "savePageSyncService"
        private const val ENQUEUE_DELAY = 2L
        const val MEDIA_LIST_PROGRESS = 30

        fun enqueue() {
            if (ReadingListSyncAdapter.inProgress()) {
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<SavedPageSyncService>()
                .setInitialDelay(ENQUEUE_DELAY, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(WikipediaApp.instance)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun sendSyncEvent(showMessage: Boolean = false) {
            // Note: this method posts from a background thread but subscribers expect events to be
            // received on the main thread.
            FlowEventBus.post(ReadingListSyncEvent(showMessage))
        }
    }
}
