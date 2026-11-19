package org.wikipedia.savedpages

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
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
import org.wikipedia.offline.ArticleCacheRequest
import org.wikipedia.offline.OfflineCacheWrite
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.readinglist.ArticleCacheEvidence
import org.wikipedia.readinglist.ArticleKey
import org.wikipedia.readinglist.articleKey
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
import java.io.File
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
            val database = AppDatabase.instance
            val pageDao = database.readingListPageDao()
            val cleanedUp = try {
                withContext(NonCancellable) {
                    val (paths, changed) = database.withTransaction {
                        val deleted = pageDao.getAllPagesToBeDeleted()
                        val unsaved = pageDao.getAllPagesToBeUnsaved()
                        val paths = database.offlineObjectDao().detachObjectsFromPages((deleted + unsaved).map { it.id })
                        if (deleted.isNotEmpty()) pageDao.purgeDeletedPages()
                        if (unsaved.isNotEmpty()) {
                            pageDao.updateStatus(
                                oldStatus = ReadingListPage.STATUS_SAVED,
                                newStatus = ReadingListPage.STATUS_QUEUE_FOR_SAVE,
                                offline = false
                            )
                        }
                        paths to (deleted.isNotEmpty() || unsaved.isNotEmpty())
                    }
                    database.offlineObjectDao().deleteUnreferencedFiles(paths)
                    changed
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                L.e(e)
                false
            }

            val pending = pageDao.getAllPagesToBeForcedSave() +
                if (allowsAutomaticDownload()) pageDao.getAllPagesToBeSaved() else emptyList()
            val queue = ArrayDeque(pending.distinctBy { it.articleKey() })
            val itemsTotal = queue.size
            var itemsSaved = 0
            try {
                itemsSaved = savePages(database, queue)
            } finally {
                if (savedPageSyncNotification.isSyncPaused()) {
                    savedPageSyncNotification.setNotificationPaused(applicationContext, itemsTotal, itemsSaved)
                } else {
                    savedPageSyncNotification.cancelNotification(applicationContext)
                    savedPageSyncNotification.setSyncCanceled(false)
                    if (cleanedUp || itemsTotal > 0) sendSyncEvent()
                }
            }
            Result.success()
        }
    }

    private fun allowsAutomaticDownload(): Boolean =
        (!Prefs.isDownloadOnlyOverWiFiEnabled || DeviceUtil.isOnWiFi) && Prefs.isDownloadingReadingListArticlesEnabled

    private suspend fun savePages(database: AppDatabase, queue: ArrayDeque<ReadingListPage>): Int {
        val itemsTotal = queue.size
        val worker = currentCoroutineContext().job
        var itemsSaved = 0
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            if (savedPageSyncNotification.isSyncPaused()) break
            if (savedPageSyncNotification.isSyncCanceled()) {
                database.readingListPageDao().markPagesForOffline(queue, offline = false, forcedSave = false)
                break
            }

            val key = queue.removeFirst().articleKey()
            var activeWrite: OfflineCacheWrite? = null
            try {
                val page = database.withTransaction {
                    val members = database.readingListPageDao().getAllPageOccurrences(key).filter { it.offline }
                    val pending = members.firstOrNull {
                        it.status == ReadingListPage.STATUS_QUEUE_FOR_FORCED_SAVE ||
                            (it.status == ReadingListPage.STATUS_QUEUE_FOR_SAVE && allowsAutomaticDownload())
                    } ?: return@withTransaction null
                    val evidence = ArticleCacheEvidence(database.objectsForArticle(key))
                    if (members.any { evidence.hasCompletedCopy(it) }) return@withTransaction null
                    activeWrite = OfflineCacheWrite.start(key, worker)
                    pending
                } ?: continue
                val write = checkNotNull(activeWrite)
                savedPageSyncNotification.setNotificationProgress(applicationContext, itemsTotal, itemsSaved)
                var permanentFailure = false
                val completed = try {
                    savePageFor(page, write)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: InterruptedException) {
                    false
                } catch (e: Exception) {
                    L.e(e)
                    permanentFailure = !ThrowableUtil.isOffline(e) && !ThrowableUtil.isTimeout(e) && !ThrowableUtil.isNetworkError(e)
                    if (permanentFailure && e !is HttpStatusException) L.logRemoteError(e)
                    false
                }
                if (ReadingListSyncAdapter.inProgress()) break
                if (savedPageSyncNotification.isSyncCanceled()) {
                    queue.addFirst(page)
                    continue
                }
                if (savedPageSyncNotification.isSyncPaused()) break
                if ((completed || permanentFailure) && finishDownload(database, write, page.takeIf { completed })) {
                    itemsSaved++
                    sendSyncEvent()
                }
            } finally {
                activeWrite?.close()
            }
        }
        return itemsSaved
    }

    private suspend fun AppDatabase.objectsForArticle(key: ArticleKey) =
        offlineObjectDao().getObjectsForArticle(key.wiki, key.lang, key.namespace, key.apiTitle)

    private suspend fun finishDownload(database: AppDatabase, write: OfflineCacheWrite, result: ReadingListPage?): Boolean =
        database.withTransaction {
            if (!write.isCurrent()) return@withTransaction false
            val pageDao = database.readingListPageDao()
            val members = pageDao.getAllPageOccurrences(write.key).filter { it.offline }
            if (members.isEmpty()) return@withTransaction false
            val catalog = database.objectsForArticle(write.key)
            val evidence = ArticleCacheEvidence(catalog)
            val targets = members.filterNot { evidence.hasCompletedCopy(it) }.map { it.id }
            if (targets.isEmpty() || !write.isCurrent()) return@withTransaction false
            if (result == null) {
                targets.chunked(900).forEach { pageDao.failOfflineDownload(it) }
                return@withTransaction false
            }
            if (!evidence.hasMandatoryResponses(result)) return@withTransaction false
            val sizeBytes = catalog.distinctBy { it.path }.sumOf {
                if (File(it.path + ".0").isFile) File(it.path + ".1").length() else 0L
            }
            if (!write.isCurrent()) return@withTransaction false
            database.offlineObjectDao().attachObjectsToPages(catalog, members.mapTo(mutableSetOf()) { it.id }, targets.toSet())
            targets.chunked(900).forEach { ids ->
                pageDao.completeOfflineDownload(ids, sizeBytes, result.displayTitle, result.description, result.thumbUrl)
            }
            true
        }

    @Throws(Exception::class)
    private suspend fun savePageFor(page: ReadingListPage, write: OfflineCacheWrite) {
        withContext(Dispatchers.IO) {
            val pageTitle = ReadingListPage.toPageTitle(page)
            val cacheRequest = ArticleCacheRequest(write.key, write)

            val summaryCall = async { ServiceFactory.getRest(pageTitle.wikiSite).getPageSummary(pageTitle.prefixedText,
                cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
                saveHeader = OfflineCacheInterceptor.SAVE_HEADER_SAVE, langHeader = pageTitle.wikiSite.languageCode,
                titleHeader = UriUtil.encodeURL(pageTitle.prefixedText), cacheRequest = cacheRequest) }

            val mediaListCall = async { ServiceFactory.getRest(pageTitle.wikiSite).getMediaList(pageTitle.prefixedText,
                cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(),
                saveHeader = OfflineCacheInterceptor.SAVE_HEADER_SAVE, langHeader = pageTitle.wikiSite.languageCode,
                titleHeader = UriUtil.encodeURL(pageTitle.prefixedText), cacheRequest = cacheRequest) }

            val mobileHTMLCall = async { reqMobileHTML(pageTitle, cacheRequest) }

            val summaryResponse = summaryCall.await()
            val mediaListResponse = mediaListCall.await()
            val mobileHTML = mobileHTMLCall.await()

            if (!write.isCurrent()) return@withContext
            page.downloadProgress = MEDIA_LIST_PROGRESS
            FlowEventBus.post(PageDownloadEvent(page))

            val fileUrls = mutableSetOf<String>()
            // download css and javascript assets
            fileUrls.addAll(PageComponentsUrlParser.parse(mobileHTML,
                pageTitle.wikiSite).filter { url -> url.isNotEmpty() })
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

            reqSaveFiles(page, pageTitle, fileUrls, cacheRequest, write)
        }
    }

    private suspend fun reqMobileHTML(pageTitle: PageTitle, cacheRequest: ArticleCacheRequest): String {
        val request = makeUrlRequest(pageTitle.wikiSite,
            ServiceFactory.getRestBasePath(pageTitle.wikiSite) + RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText),
            pageTitle, cacheRequest).build()
        return withContext(Dispatchers.IO) {
            OkHttpConnectionFactory.client.newCall(request).execute().use {
                if (!it.isSuccessful) throw HttpStatusException(it)
                it.body.string()
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private suspend fun reqSaveFiles(page: ReadingListPage, pageTitle: PageTitle, urls: Set<String>,
                                     cacheRequest: ArticleCacheRequest, write: OfflineCacheWrite) {
        var percentage = MEDIA_LIST_PROGRESS.toFloat()
        val updateRate = (CircularProgressBar.MAX_PROGRESS - percentage) / urls.size

        for (url in urls) {
            currentCoroutineContext().ensureActive()
            if (!write.isCurrent() || savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw InterruptedException("Download no longer active.")
            }
            try {
                reqSaveUrl(pageTitle, page.wiki, url, cacheRequest)
                percentage += updateRate
                page.downloadProgress = percentage.toInt()
                if (write.isCurrent()) FlowEventBus.post(PageDownloadEvent(page))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isRetryable(e)) throw e
            }
        }
        page.downloadProgress = CircularProgressBar.MAX_PROGRESS
        if (write.isCurrent()) FlowEventBus.post(PageDownloadEvent(page))
    }

    @Throws(IOException::class)
    private suspend fun reqSaveUrl(pageTitle: PageTitle, wiki: WikiSite, url: String, cacheRequest: ArticleCacheRequest) {
        val request = makeUrlRequest(wiki, url, pageTitle, cacheRequest).build()
        withContext(Dispatchers.IO) {
            OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                // Read the entirety of the response, so that it's written to cache by the interceptor.
                response.body.source().readAll(object : Sink {
                    override fun write(source: Buffer, byteCount: Long) {}
                    override fun flush() {}
                    override fun timeout(): Timeout = Timeout()
                    override fun close() {}
                })
            }
        }
    }

    private fun makeUrlRequest(wiki: WikiSite, url: String, pageTitle: PageTitle, cacheRequest: ArticleCacheRequest): Request.Builder {
        return Request.Builder().cacheControl(OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK)
            .url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
            .addHeader("Accept-Language", WikipediaApp.instance.getAcceptLanguage(pageTitle.wikiSite))
            .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            .addHeader(OfflineCacheInterceptor.LANG_HEADER, pageTitle.wikiSite.languageCode)
            .addHeader(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(pageTitle.prefixedText))
            .tag(ArticleCacheRequest::class.java, cacheRequest)
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
