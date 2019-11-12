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
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.events.PageDownloadEvent;
import org.wikipedia.gallery.MediaList;
import org.wikipedia.html.ImageTagParser;
import org.wikipedia.html.PixelDensityDescriptorParser;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.references.References;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;

import static org.wikipedia.views.CircularProgressBar.MAX_PROGRESS;

public class SavedPageSyncService extends JobIntentService {
    // Unique job ID for this service (do not duplicate).
    private static final int JOB_ID = 1000;
    private static final int ENQUEUE_DELAY_MILLIS = 2000;
    public static final int LEAD_SECTION_PROGRESS = 25;
    public static final int MOBILE_HTML_SECTION_PROGRESS = 50;
    public static final int MEDIA_LIST_PROGRESS = 60;
    public static final int REFERENCES_PROGRESS = 70;

    private static Runnable ENQUEUE_RUNNABLE = () -> enqueueWork(WikipediaApp.getInstance(),
            SavedPageSyncService.class, JOB_ID, new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));

    @NonNull private final PageImageUrlParser pageImageUrlParser
            = new PageImageUrlParser(new ImageTagParser(), new PixelDensityDescriptorParser());
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
        // Note: this method posts from a background thread but subscribers expect events to be
        // received on the main thread.
        WikipediaApp.getInstance().getBus().post(new ReadingListSyncEvent());
    }

    @SuppressLint("CheckResult")
    private void deletePageContents(@NonNull ReadingListPage page) {
        PageTitle pageTitle = ReadingListPage.toPageTitle(page);
        Observable.zip(reqPageLead(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle),
                reqMediaList(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle),
                reqPageReferences(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle), (leadRsp, mediaListRsp, referencesRsp) -> {
                    reqMobileHTML(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle);
                    Set<String> imageUrls = new HashSet<>();
                    if (leadRsp.body() != null) {
                        imageUrls.addAll(pageImageUrlParser.parse(leadRsp.body()));
                        if (!TextUtils.isEmpty(pageTitle.getThumbUrl())) {
                            imageUrls.add(pageTitle.getThumbUrl());
                        }
                    }
                    return imageUrls;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(imageUrls -> {
                    for (String url : imageUrls) {
                        Request request = makeUrlRequest(CacheControl.FORCE_CACHE, pageTitle.getWikiSite(), url)
                                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_DELETE)
                                .build();
                        try {
                            OkHttpConnectionFactory.getClient().newCall(request).execute();
                        } catch (Exception e) {
                            // ignore exceptions while deleting cached items.
                        }
                    }
                }, L::d);
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

        Observable<retrofit2.Response<PageLead>> leadCall = reqPageLead(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);
        Observable<retrofit2.Response<MediaList>> mediaListCall = reqMediaList(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);
        Observable<retrofit2.Response<References>> referencesCall = reqPageReferences(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);
        final Long[] pageSize = new Long[1];
        final Exception[] exception = new Exception[1];

        Observable.zip(leadCall, mediaListCall, referencesCall, (leadRsp, mediaListRsp, referencesRsp) -> {
            long totalSize = 0;
            totalSize += responseSize(leadRsp);
            page.downloadProgress(LEAD_SECTION_PROGRESS);
            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
            totalSize += reqMobileHTML(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);
            page.downloadProgress(MOBILE_HTML_SECTION_PROGRESS);
            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
            totalSize += responseSize(mediaListRsp);
            page.downloadProgress(MEDIA_LIST_PROGRESS);
            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
            totalSize += responseSize(referencesRsp);
            page.downloadProgress(REFERENCES_PROGRESS);
            WikipediaApp.getInstance().getBus().post(new PageDownloadEvent(page));
            Set<String> imageUrls = new HashSet<>(pageImageUrlParser.parse(leadRsp.body()));

            if (!TextUtils.isEmpty(leadRsp.body().getThumbUrl())) {
                page.thumbUrl(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                        leadRsp.body().getThumbUrl()));
                persistPageThumbnail(pageTitle, page.thumbUrl());
                imageUrls.add(page.thumbUrl());
            }
            page.description(leadRsp.body().getDescription());

            if (Prefs.isImageDownloadEnabled()) {
                totalSize += reqSaveImages(page, imageUrls, REFERENCES_PROGRESS, MAX_PROGRESS);
            }

            String title = pageTitle.getPrefixedText();
            L.i("Saved page " + title + " (" + totalSize + ")");

            return totalSize;
        }).subscribeOn(Schedulers.io())
                .blockingSubscribe(size -> pageSize[0] = size,
                        t -> exception[0] = (Exception) t);
        if (exception[0] != null) {
            throw exception[0];
        }
        return pageSize[0];
    }

    // TODO: will be replaced by metadata and summary endpoints
    @NonNull
    private Observable<retrofit2.Response<PageLead>> reqPageLead(@NonNull CacheControl cacheControl,
                                                                          @NonNull String saveOfflineHeader,
                                                                          @NonNull PageTitle pageTitle) {
        String title = pageTitle.getPrefixedText();
        int thumbnailWidth = DimenUtil.calculateLeadImageWidth();
        return new PageClient().lead(pageTitle.getWikiSite(), cacheControl, saveOfflineHeader, null, title, thumbnailWidth);
    }

    @NonNull
    private Observable<retrofit2.Response<MediaList>> reqMediaList(@NonNull CacheControl cacheControl,
                                                                   @NonNull String saveOfflineHeader,
                                                                   @NonNull PageTitle pageTitle) {
        // TODO: query the image urls and will be able to show the images for offline reading
        return ServiceFactory.getRest(pageTitle.getWikiSite()).getMediaList(cacheControl.toString(), saveOfflineHeader, pageTitle.getConvertedText());
    }

    @NonNull
    private Observable<retrofit2.Response<References>> reqPageReferences(@NonNull CacheControl cacheControl,
                                                                                  @NonNull String saveOfflineHeader,
                                                                                  @NonNull PageTitle pageTitle) {
        return ServiceFactory.getRest(pageTitle.getWikiSite()).getReferences(cacheControl.toString(), saveOfflineHeader, pageTitle.getConvertedText());
    }

    private long reqMobileHTML(@NonNull CacheControl cacheControl,
                               @NonNull String saveOfflineHeader,
                               @NonNull PageTitle pageTitle) throws IOException {
        long downloadSize = 0;
        Request request = makeUrlRequest(cacheControl, pageTitle.getWikiSite(),
                pageTitle.getWikiSite().url() + RestService.REST_API_PREFIX + RestService.PAGE_HTML_ENDPOINT + pageTitle.getConvertedText())
                .addHeader("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(pageTitle.getWikiSite()))
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, saveOfflineHeader)
                .build();

        Response rsp = OkHttpConnectionFactory.getClient().newCall(request).execute();

        // Don't delete CSS and JavaScript files since they are shared.
        if (rsp.body() != null && !saveOfflineHeader.equals(OfflineCacheInterceptor.SAVE_HEADER_DELETE)) {
            String body = rsp.body().string();
            List<String> componentsUrls = new PageComponentsUrlParser().parse(body);
            for (String url : componentsUrls) {
                downloadSize += reqSaveUrl(cacheControl, saveOfflineHeader, pageTitle.getWikiSite(), url);
            }
        }

        downloadSize += responseSize(rsp);

        return downloadSize;
    }

    private long reqSaveImages(@NonNull ReadingListPage page, @NonNull Set<String> urls, int progressStart, int progressEnd) throws IOException, InterruptedException {
        int numOfImages = urls.size();
        long totalSize = 0;
        float percentage = progressStart;
        float updateRate = (progressEnd - percentage) / numOfImages;
        for (String url : urls) {
            if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw new InterruptedException("Sync paused or cancelled.");
            }
            try {
                totalSize += reqSaveUrl(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, page.wiki(), url);
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

        return totalSize;
    }

    private long reqSaveUrl(@NonNull CacheControl cacheControl,
                            @NonNull String saveOfflineHeader,
                            @NonNull WikiSite wiki,
                            @NonNull String url) throws IOException {
        Request request = makeUrlRequest(cacheControl, wiki, url)
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, saveOfflineHeader)
                .build();

        Response rsp = OkHttpConnectionFactory.getClient().newCall(request).execute();

        // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is read
        // for the cache to be written.
        rsp.body().close();

        // Size must be checked after the body has been written.
        return responseSize(rsp);
    }

    @NonNull private Request.Builder makeUrlRequest(@NonNull CacheControl cacheControl, @NonNull WikiSite wiki, @NonNull String url) {
        return new Request.Builder().cacheControl(cacheControl).url(UriUtil.resolveProtocolRelativeUrl(wiki, url));
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

    private long responseSize(@NonNull Response rsp) {
        return OkHttpConnectionFactory.SAVE_CACHE.getSizeOnDisk(rsp.request());
    }

    private long responseSize(@NonNull retrofit2.Response rsp) {
        return OkHttpConnectionFactory.SAVE_CACHE.getSizeOnDisk(rsp.raw().request());
    }
}
