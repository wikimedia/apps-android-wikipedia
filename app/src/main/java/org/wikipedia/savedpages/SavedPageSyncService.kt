package org.wikipedia.savedpages

import android.content.Intent
import androidx.core.app.JobIntentService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Request
import okio.Buffer
import okio.Sink
import okio.Timeout
import org.wikipedia.WikipediaApp
import org.wikipedia.database.contract.PageImageHistoryContract
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.gallery.MediaList
import org.wikipedia.offline.OfflineObjectDbHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.PageImage
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar
import retrofit2.Response
import java.io.IOException

class SavedPageSyncService : JobIntentService() {
    private val savedPageSyncNotification = SavedPageSyncNotification.instance
    val app: WikipediaApp = WikipediaApp.getInstance()

    override fun onHandleWork(intent: Intent) {
        if (ReadingListSyncAdapter.inProgress()) {
            // Reading list sync was started in the meantime, so bail.
            return
        }
        val pagesToSave = ReadingListDbHelper.allPagesToBeForcedSave
        if ((!Prefs.isDownloadOnlyOverWiFiEnabled() || DeviceUtil.isOnWiFi()) &&
                Prefs.isDownloadingReadingListArticlesEnabled()) {
            pagesToSave.addAll(ReadingListDbHelper.allPagesToBeSaved)
        }
        val pagesToUnSave = ReadingListDbHelper.allPagesToBeUnsaved
        val pagesToDelete = ReadingListDbHelper.allPagesToBeDeleted
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
                ReadingListDbHelper.purgeDeletedPages()
                shouldSendSyncEvent = true
            }
            if (pagesToUnSave.isNotEmpty()) {
                ReadingListDbHelper.resetUnsavedPageStatus()
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
    }

    private fun deletePageContents(page: ReadingListPage) {
        Completable.fromAction { OfflineObjectDbHelper.instance().deleteObjectsForPageId(page.id) }.subscribeOn(Schedulers.io())
                .subscribe({}) { obj -> L.e(obj) }
    }

    private fun savePages(queue: MutableList<ReadingListPage>): Int {
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
                ReadingListDbHelper.markPagesForOffline(queue, offline = false, forcedSave = false)
                break
            }
            savedPageSyncNotification.setNotificationProgress(applicationContext, itemsTotal, itemsSaved)
            var success = false
            var totalSize = 0L
            try {
                // Lengthy operation during which the db state may change...
                totalSize = savePageFor(page)
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
                ReadingListDbHelper.updatePage(page)
                itemsSaved++
                sendSyncEvent()
            }
        }
        return itemsSaved
    }

    @Throws(Exception::class)
    private fun savePageFor(page: ReadingListPage): Long {
        val pageTitle = ReadingListPage.toPageTitle(page)
        var pageSize = 0L
        var exception: Exception? = null
        reqPageSummary(pageTitle)
                .flatMap { rsp ->
                    val revision = if (rsp.body() != null) rsp.body()!!.revision else 0
                    Observable.zip(Observable.just(rsp),
                            reqMediaList(pageTitle, revision),
                            reqMobileHTML(pageTitle)) { summaryRsp, mediaListRsp, mobileHTMLRsp ->
                        page.downloadProgress = SUMMARY_PROGRESS
                        app.bus.post(PageDownloadEvent(page))
                        page.downloadProgress = MOBILE_HTML_SECTION_PROGRESS
                        app.bus.post(PageDownloadEvent(page))
                        page.downloadProgress = MEDIA_LIST_PROGRESS
                        app.bus.post(PageDownloadEvent(page))
                        val fileUrls = mutableSetOf<String>()

                        // download css and javascript assets
                        mobileHTMLRsp.body?.let {
                            fileUrls.addAll(PageComponentsUrlParser.parse(it.string(),
                                    pageTitle.wikiSite).filter { url -> url.isNotEmpty() })
                        }
                        if (Prefs.isImageDownloadEnabled()) {
                            // download thumbnail and lead image
                            if (!summaryRsp.body()!!.thumbnailUrl.isNullOrEmpty()) {
                                page.thumbUrl = UriUtil.resolveProtocolRelativeUrl(pageTitle.wikiSite,
                                        summaryRsp.body()!!.thumbnailUrl!!)
                                persistPageThumbnail(pageTitle, page.thumbUrl!!)
                                fileUrls.add(UriUtil.resolveProtocolRelativeUrl(
                                        ImageUrlUtil.getUrlForPreferredSize(page.thumbUrl!!, DimenUtil.calculateLeadImageWidth())))
                            }

                            // download article images
                            for (item in mediaListRsp.body()!!.getItems("image")) {
                                if (item.srcSets.isNotEmpty()) {
                                    fileUrls.add(item.getImageUrl(DimenUtil.densityScalar))
                                }
                            }
                        }
                        page.displayTitle = summaryRsp.body()!!.displayTitle
                        page.description = summaryRsp.body()!!.description
                        reqSaveFiles(page, pageTitle, fileUrls)
                        val totalSize = OfflineObjectDbHelper.instance().getTotalBytesForPageId(page.id)
                        L.i("Saved page " + pageTitle.prefixedText + " (" + totalSize + ")")
                        totalSize
                    }
                }
                .subscribeOn(Schedulers.io())
                .blockingSubscribe({ size -> pageSize = size }) { t -> exception = t as Exception }

        exception?.let {
            throw it
        }

        return pageSize
    }

    private fun reqPageSummary(pageTitle: PageTitle): Observable<Response<PageSummary?>> {
        return ServiceFactory.getRest(pageTitle.wikiSite).getSummaryResponse(pageTitle.prefixedText,
                null, CACHE_CONTROL_FORCE_NETWORK.toString(),
                OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode(),
                UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private fun reqMediaList(pageTitle: PageTitle, revision: Long): Observable<Response<MediaList>> {
        return ServiceFactory.getRest(pageTitle.wikiSite).getMediaListResponse(pageTitle.prefixedText,
                revision, CACHE_CONTROL_FORCE_NETWORK.toString(),
                OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode(),
                UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private fun reqMobileHTML(pageTitle: PageTitle): Observable<okhttp3.Response> {
        val request: Request = makeUrlRequest(pageTitle.wikiSite,
                ServiceFactory.getRestBasePath(pageTitle.wikiSite) +
                        RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText),
                pageTitle).build()
        return Observable.create { emitter ->
            try {
                if (!emitter.isDisposed) {
                    emitter.onNext(client.newCall(request).execute())
                    emitter.onComplete()
                }
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun reqSaveFiles(page: ReadingListPage, pageTitle: PageTitle, urls: Set<String>) {
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
    private fun reqSaveUrl(pageTitle: PageTitle, wiki: WikiSite, url: String) {
        val request = makeUrlRequest(wiki, url, pageTitle).build()
        val rsp = client.newCall(request).execute()

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
        return Request.Builder().cacheControl(CACHE_CONTROL_FORCE_NETWORK).url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
                .addHeader("Accept-Language", app.getAcceptLanguage(pageTitle.wikiSite))
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
                .addHeader(OfflineCacheInterceptor.LANG_HEADER, pageTitle.wikiSite.languageCode())
                .addHeader(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(pageTitle.prefixedText))
    }

    private fun persistPageThumbnail(title: PageTitle, url: String) {
        app.getDatabaseClient(PageImage::class.java).upsert(
                PageImage(title, url), PageImageHistoryContract.Image.SELECTION)
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
        // Unique job ID for this service (do not duplicate).
        private const val JOB_ID = 1000
        private const val ENQUEUE_DELAY_MILLIS = 2000
        const val SUMMARY_PROGRESS = 10
        const val MOBILE_HTML_SECTION_PROGRESS = 20
        const val MEDIA_LIST_PROGRESS = 30

        private val ENQUEUE_RUNNABLE = Runnable {
            enqueueWork(WikipediaApp.getInstance(),
                    SavedPageSyncService::class.java, JOB_ID, Intent(WikipediaApp.getInstance(), SavedPageSyncService::class.java))
        }

        @JvmStatic
        fun enqueue() {
            if (ReadingListSyncAdapter.inProgress()) {
                return
            }
            WikipediaApp.getInstance().mainThreadHandler.removeCallbacks(ENQUEUE_RUNNABLE)
            WikipediaApp.getInstance().mainThreadHandler.postDelayed(ENQUEUE_RUNNABLE, ENQUEUE_DELAY_MILLIS.toLong())
        }

        @JvmStatic
        @JvmOverloads
        fun sendSyncEvent(showMessage: Boolean = false) {
            // Note: this method posts from a background thread but subscribers expect events to be
            // received on the main thread.
            WikipediaApp.getInstance().bus.post(ReadingListSyncEvent(showMessage))
        }
    }
}
