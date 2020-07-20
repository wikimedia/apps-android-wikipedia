package org.wikipedia.savedpages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.events.PageDownloadEvent;
import org.wikipedia.gallery.MediaList;
import org.wikipedia.gallery.MediaListItem;
import org.wikipedia.offline.OfflineObjectDbHelper;
import org.wikipedia.page.PageTitle;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.Sink;
import okio.Timeout;

import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK;
import static org.wikipedia.views.CircularProgressBar.MAX_PROGRESS;

public class SavedPageSyncService extends JobIntentService {
    // Unique job ID for this service (do not duplicate).
    private static final int JOB_ID = 1000;
    private static final int ENQUEUE_DELAY_MILLIS = 2000;
    public static final int SUMMARY_PROGRESS = 10;
    public static final int MOBILE_HTML_SECTION_PROGRESS = 20;
    public static final int MEDIA_LIST_PROGRESS = 30;

    private static Runnable ENQUEUE_RUNNABLE = () -> enqueueWork(WikipediaApp.getInstance(),
            SavedPageSyncService.class, JOB_ID, new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));

    private SavedPageSyncNotification savedPageSyncNotification;

    public SavedPageSyncService() {
        savedPageSyncNotification = SavedPageSyncNotification.getInstance();
    }

    public static void enqueue() {
        if (ReadingListSyncAdapter.inProgress()) {
            return;
        }
        WikipediaApp.getInstance().getMainThreadHandler().removeCallbacks(ENQUEUE_RUNNABLE);
        WikipediaApp.getInstance().getMainThreadHandler().postDelayed(ENQUEUE_RUNNABLE, ENQUEUE_DELAY_MILLIS);
    }

    @Override protected void onHandleWork(@NonNull Intent intent) {
        if (ReadingListSyncAdapter.inProgress()) {
            // Reading list sync was started in the meantime, so bail.
            return;
        }

        List<ReadingListPage> pagesToSave = ReadingListDbHelper.instance().getAllPagesToBeForcedSave();
        if ((!Prefs.isDownloadOnlyOverWiFiEnabled() || DeviceUtil.isOnWiFi())
                && Prefs.isDownloadingReadingListArticlesEnabled()) {
            pagesToSave.addAll(ReadingListDbHelper.instance().getAllPagesToBeSaved());
        }
        List<ReadingListPage> pagesToUnsave = ReadingListDbHelper.instance().getAllPagesToBeUnsaved();
        List<ReadingListPage> pagesToDelete = ReadingListDbHelper.instance().getAllPagesToBeDeleted();
        boolean shouldSendSyncEvent = false;

        try {
            for (ReadingListPage page : pagesToDelete) {
                deletePageContents(page);
            }
            for (ReadingListPage page : pagesToUnsave) {
                deletePageContents(page);
            }
        } catch (Exception e) {
            L.e("Error while deleting page: " + e.getMessage());
        } finally {
            if (!pagesToDelete.isEmpty()) {
                ReadingListDbHelper.instance().purgeDeletedPages();
                shouldSendSyncEvent = true;
            }
            if (!pagesToUnsave.isEmpty()) {
                ReadingListDbHelper.instance().resetUnsavedPageStatus();
                shouldSendSyncEvent = true;
            }
        }

        int itemsTotal = pagesToSave.size();
        if (itemsTotal > 0) {
            shouldSendSyncEvent = true;
        }
        int itemsSaved = 0;
        try {
            itemsSaved = savePages(pagesToSave);
        } finally {
            if (savedPageSyncNotification.isSyncPaused()) {
                savedPageSyncNotification.setNotificationPaused(getApplicationContext(), itemsTotal, itemsSaved);
            } else {
                savedPageSyncNotification.cancelNotification(getApplicationContext());
                savedPageSyncNotification.setSyncCanceled(false);
                if (shouldSendSyncEvent) {
                    sendSyncEvent();
                }
            }
        }
    }

    public static void sendSyncEvent() {
        sendSyncEvent(false);
    }

    public static void sendSyncEvent(boolean showMessage) {
        // Note: this method posts from a background thread but subscribers expect events to be
        // received on the main thread.
        WikipediaApp.getInstance().getBus().post(new ReadingListSyncEvent(showMessage));
    }

    @SuppressLint("CheckResult")
    private void deletePageContents(@NonNull ReadingListPage page) {
        Completable.fromAction(() -> OfflineObjectDbHelper.instance().deleteObjectsForPageId(page.id())).subscribeOn(Schedulers.io())
                .subscribe(() -> { }, L::e);
    }

    private int savePages(List<ReadingListPage> queue) {
        int itemsTotal = queue.size();
        int itemsSaved = 0;
        while (!queue.isEmpty()) {

            // Pick off the DB row that we'll be working on...
            ReadingListPage page = queue.remove(0);

            if (savedPageSyncNotification.isSyncPaused()) {
                // Remaining transactions will be picked up again when the service is resumed.
                break;
            } else if (savedPageSyncNotification.isSyncCanceled()) {
                // Mark remaining pages as online-only!
                queue.add(page);
                ReadingListDbHelper.instance().markPagesForOffline(queue, false, false);
                break;
            }
            savedPageSyncNotification.setNotificationProgress(getApplicationContext(), itemsTotal, itemsSaved);

            boolean success = false;
            long totalSize = 0;
            try {

                // Lengthy operation during which the db state may change...
                totalSize = savePageFor(page);
                success = true;

            } catch (InterruptedException e) {
                // fall through
            } catch (Exception e) {
                // This can be an IOException from the storage media, or several types
                // of network exceptions from malformed URLs, timeouts, etc.
                e.printStackTrace();

                // If we're offline, or if there's a transient network error, then don't do
                // anything.  Otherwise...
                if (!ThrowableUtil.isOffline(e) && !ThrowableUtil.isTimeout(e) && !ThrowableUtil.isNetworkError(e)) {
                    // If it's not a transient network error (e.g. a 404 status response), it implies
                    // that there's no way to fetch the page next time, or ever, therefore let's mark
                    // it as "successful" so that it won't be retried again.
                    success = true;
                    if (!(e instanceof HttpStatusException)) {
                        // And if it's something other than an HTTP status, let's log it and see what it is.
                        L.logRemoteError(e);
                    }
                }
            }

            if (ReadingListSyncAdapter.inProgress()) {
                // Reading list sync was started in the meantime, so bail.
                break;
            }

            if (success) {
                page.status(ReadingListPage.STATUS_SAVED);
                page.sizeBytes(totalSize);
                ReadingListDbHelper.instance().updatePage(page);
                itemsSaved++;
                sendSyncEvent();
            }
        }
        return itemsSaved;
    }

    private long savePageFor(@NonNull ReadingListPage page) throws Exception {
        PageTitle pageTitle = ReadingListPage.toPageTitle(page);

        final Long[] pageSize = new Long[1];
        final Exception[] exception = new Exception[1];

        reqPageSummary(pageTitle)
                .flatMap(rsp -> {
                long revision = rsp.body() != null ? rsp.body().getRevision() : 0;
                return Observable.zip(Observable.just(rsp),
                        reqMediaList(pageTitle, revision),
                        reqMobileHTML(pageTitle), (summaryRsp, mediaListRsp, mobileHTMLRsp) -> {
                            page.downloadProgress(SUMMARY_PROGRESS);
                            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
                            page.downloadProgress(MOBILE_HTML_SECTION_PROGRESS);
                            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
                            page.downloadProgress(MEDIA_LIST_PROGRESS);
                            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
                            Set<String> fileUrls = new HashSet<>();

                            // download css and javascript assets
                            if (mobileHTMLRsp.body() != null) {
                                String body = mobileHTMLRsp.body().string();
                                List<String> componentsUrls = new PageComponentsUrlParser().parse(body, pageTitle.getWikiSite());
                                for (String url : componentsUrls) {
                                    if (!TextUtils.isEmpty(url)) {
                                        fileUrls.add(url);
                                    }
                                }
                            }

                            if (Prefs.isImageDownloadEnabled()) {
                                // download thumbnail and lead image
                                if (!TextUtils.isEmpty(summaryRsp.body().getThumbnailUrl())) {
                                    page.thumbUrl(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                                            summaryRsp.body().getThumbnailUrl()));
                                    persistPageThumbnail(pageTitle, page.thumbUrl());
                                    fileUrls.add(UriUtil.resolveProtocolRelativeUrl(ImageUrlUtil
                                            .getUrlForPreferredSize(page.thumbUrl(), DimenUtil.calculateLeadImageWidth())));
                                }

                                // download article images
                                for (MediaListItem item : mediaListRsp.body().getItems("image")) {
                                    if (!item.getSrcSets().isEmpty()) {
                                        fileUrls.add(item.getImageUrl(DimenUtil.getDensityScalar()));
                                    }
                                }
                            }

                            page.title(summaryRsp.body().getDisplayTitle());
                            page.description(summaryRsp.body().getDescription());

                            reqSaveFiles(page, pageTitle, fileUrls, MEDIA_LIST_PROGRESS, MAX_PROGRESS);

                            long totalSize = OfflineObjectDbHelper.instance().getTotalBytesForPageId(page.id());

                            L.i("Saved page " + pageTitle.getPrefixedText() + " (" + totalSize + ")");
                            return totalSize;
                        });
                })
                .subscribeOn(Schedulers.io())
                .blockingSubscribe(size -> pageSize[0] = size, t -> exception[0] = (Exception) t);

        if (exception[0] != null) {
            throw exception[0];
        }

        return pageSize[0];
    }

    @NonNull
    private Observable<retrofit2.Response<PageSummary>> reqPageSummary(@NonNull PageTitle pageTitle) {
        return ServiceFactory.getRest(pageTitle.getWikiSite()).getSummaryResponse(pageTitle.getPrefixedText(), null, CACHE_CONTROL_FORCE_NETWORK.toString(),
                OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.getWikiSite().languageCode(), UriUtil.encodeURL(pageTitle.getPrefixedText()));
    }

    @NonNull
    private Observable<retrofit2.Response<MediaList>> reqMediaList(@NonNull PageTitle pageTitle, long revision) {
        return ServiceFactory.getRest(pageTitle.getWikiSite()).getMediaListResponse(pageTitle.getPrefixedText(), revision, CACHE_CONTROL_FORCE_NETWORK.toString(),
                OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.getWikiSite().languageCode(), UriUtil.encodeURL(pageTitle.getPrefixedText()));
    }

    private Observable<okhttp3.Response> reqMobileHTML(@NonNull PageTitle pageTitle) {
        Request request = makeUrlRequest(pageTitle.getWikiSite(), ServiceFactory.getRestBasePath(pageTitle.getWikiSite())
                + RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.getPrefixedText()), pageTitle).build();

        return Observable.create(emitter -> {
            try {
                if (!emitter.isDisposed()) {
                    emitter.onNext(OkHttpConnectionFactory.getClient().newCall(request).execute());
                    emitter.onComplete();
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    private void reqSaveFiles(@NonNull ReadingListPage page, @NonNull PageTitle pageTitle, @NonNull Set<String> urls,
                              int progressStart, int progressEnd) throws IOException, InterruptedException {
        int numOfImages = urls.size();
        float percentage = progressStart;
        float updateRate = (progressEnd - percentage) / numOfImages;
        for (String url : urls) {
            if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw new InterruptedException("Sync paused or cancelled.");
            }
            try {
                reqSaveUrl(pageTitle, page.wiki(), url);
                percentage += updateRate;
                page.downloadProgress((int) percentage);
                WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));

            } catch (Exception e) {
                if (isRetryable(e)) {
                    throw e;
                }
            }
        }
        page.downloadProgress(progressEnd);
        WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
    }

    private void reqSaveUrl(@NonNull PageTitle pageTitle, @NonNull WikiSite wiki, @NonNull String url) throws IOException {
        Request request = makeUrlRequest(wiki, url, pageTitle).build();
        Response rsp = OkHttpConnectionFactory.getClient().newCall(request).execute();

        // Read the entirety of the response, so that it's written to cache by the interceptor.
        rsp.body().source().readAll(new Sink() {
            @Override public void write(Buffer buffer, long l) { }
            @Override public void flush() { }
            @Override public Timeout timeout() {
                return new Timeout();
            }
            @Override public void close() { }
        });
        rsp.body().close();
    }

    @NonNull private Request.Builder makeUrlRequest(@NonNull WikiSite wiki, @NonNull String url, @NonNull PageTitle pageTitle) {
        return new Request.Builder().cacheControl(CACHE_CONTROL_FORCE_NETWORK).url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
                .addHeader("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(pageTitle.getWikiSite()))
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
                .addHeader(OfflineCacheInterceptor.LANG_HEADER, pageTitle.getWikiSite().languageCode())
                .addHeader(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(pageTitle.getPrefixedText()));
    }

    private void persistPageThumbnail(@NonNull PageTitle title, @NonNull String url) {
        WikipediaApp.getInstance().getDatabaseClient(PageImage.class).upsert(
                new PageImage(title, url), PageImageHistoryContract.Image.SELECTION);
    }

    private boolean isRetryable(@NonNull Throwable t) {
        //"Retryable" in this case refers to exceptions that will be rethrown up to the
        //outer exception handler, so that the entire page can be retried on the next pass
        //of the sync service.
        //Errors that do *not* qualify for retrying include:
        //- IllegalArgumentException (thrown for any kind of malformed URL)
        //- HTTP 404 status (for nonexistent media)
        return !(t instanceof IllegalArgumentException
                || ThrowableUtil.is404(t));
    }
}
