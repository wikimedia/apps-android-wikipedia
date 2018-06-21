package org.wikipedia.savedpages;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.html.ImageTagParser;
import org.wikipedia.html.PixelDensityDescriptorParser;
import org.wikipedia.page.PageTitle;
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

import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

public class SavedPageSyncService extends JobIntentService {
    // Unique job ID for this service (do not duplicate).
    private static final int JOB_ID = 1000;
    private static final int ENQUEUE_DELAY_MILLIS = 2000;
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
        if (!Prefs.isDownloadOnlyOverWiFiEnabled() || DeviceUtil.isOnWiFi()) {
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

    private void deletePageContents(@NonNull ReadingListPage page) {
        PageTitle pageTitle = ReadingListPage.toPageTitle(page);
        PageLead lead = null;
        Call<PageLead> leadCall = reqPageLead(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle);
        try {
            lead = leadCall.execute().body();
        } catch (IOException ignore) { }

        if (lead != null) {
            List<String> imageUrls = pageImageUrlParser.parse(lead);
            if (!TextUtils.isEmpty(pageTitle.getThumbUrl())) {
                imageUrls.add(pageTitle.getThumbUrl());
            }
            for (String url : imageUrls) {
                try {
                    Request request = makeImageRequest(pageTitle.getWikiSite(), url)
                            .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_DELETE)
                            .build();
                    OkHttpConnectionFactory.getClient().newCall(request).execute();
                } catch (IOException ignore) { }
            }
        }

        Call<PageRemaining> sectionsCall = reqPageSections(CacheControl.FORCE_CACHE, OfflineCacheInterceptor.SAVE_HEADER_DELETE, pageTitle);
        PageRemaining sections = null;
        try {
            sections = sectionsCall.execute().body();
        } catch (IOException ignore) { }

        if (sections != null) {
            for (String url : pageImageUrlParser.parse(sections)) {
                try {
                    Request request = makeImageRequest(pageTitle.getWikiSite(), url)
                            .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_DELETE)
                            .build();
                    OkHttpConnectionFactory.getClient().newCall(request).execute();
                } catch (IOException ignore) { }
            }
        }
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
                if (!ThrowableUtil.isOffline(e) && !ThrowableUtil.isNetworkError(e)) {
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

    private long savePageFor(@NonNull ReadingListPage page) throws IOException, InterruptedException {
        PageTitle pageTitle = ReadingListPage.toPageTitle(page);
        long totalSize = 0;

        Call<PageLead> leadCall = reqPageLead(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);
        Call<PageRemaining> sectionsCall = reqPageSections(CacheControl.FORCE_NETWORK, OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle);

        retrofit2.Response<PageLead> leadRsp = leadCall.execute();
        totalSize += responseSize(leadRsp);
        retrofit2.Response<PageRemaining> sectionsRsp = sectionsCall.execute();
        totalSize += responseSize(sectionsRsp);

        Set<String> imageUrls = new HashSet<>(pageImageUrlParser.parse(leadRsp.body()));
        imageUrls.addAll(pageImageUrlParser.parse(sectionsRsp.body()));

        if (!TextUtils.isEmpty(leadRsp.body().getThumbUrl())) {
            page.thumbUrl(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                    leadRsp.body().getThumbUrl()));
            persistPageThumbnail(pageTitle, page.thumbUrl());
            imageUrls.add(page.thumbUrl());
        }
        page.description(leadRsp.body().getDescription());

        if (Prefs.isImageDownloadEnabled()) {
            totalSize += reqSaveImages(pageTitle.getWikiSite(), imageUrls);
        }

        String title = pageTitle.getPrefixedText();
        L.i("Saved page " + title + " (" + totalSize + ")");

        return totalSize;
    }

    @NonNull private Call<PageLead> reqPageLead(@Nullable CacheControl cacheControl,
                                                @Nullable String saveOfflineHeader,
                                                @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);
        String title = pageTitle.getPrefixedText();
        int thumbnailWidth = DimenUtil.calculateLeadImageWidth();
        return client.lead(cacheControl, saveOfflineHeader, null, title, thumbnailWidth);
    }

    @NonNull private Call<PageRemaining> reqPageSections(@Nullable CacheControl cacheControl,
                                                         @Nullable String saveOfflineHeader,
                                                         @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);
        String title = pageTitle.getPrefixedText();
        return client.sections(cacheControl, saveOfflineHeader, title);
    }

    private long reqSaveImages(@NonNull WikiSite wiki, @NonNull Iterable<String> urls) throws IOException, InterruptedException {
        long totalSize = 0;
        for (String url : urls) {
            if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw new InterruptedException("Sync paused or cancelled.");
            }
            try {
                totalSize += reqSaveImage(wiki, url);
            } catch (Exception e) {
                if (isRetryable(e)) {
                    throw e;
                }
            }
        }
        return totalSize;
    }

    private long reqSaveImage(@NonNull WikiSite wiki, @NonNull String url) throws IOException {
        Request request = makeImageRequest(wiki, url)
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
                .build();

        Response rsp = OkHttpConnectionFactory.getClient().newCall(request).execute();

        // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is read
        // for the cache to be written.
        rsp.body().close();

        // Size must be checked after the body has been written.
        return responseSize(rsp);
    }

    @NonNull private Request.Builder makeImageRequest(@NonNull WikiSite wiki, @NonNull String url) {
        return new Request.Builder()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .url(UriUtil.resolveProtocolRelativeUrl(wiki, url));
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

    @NonNull private PageClient newPageClient(@NonNull PageTitle title) {
        return PageClientFactory.create(title.getWikiSite(), title.namespace());
    }
}
