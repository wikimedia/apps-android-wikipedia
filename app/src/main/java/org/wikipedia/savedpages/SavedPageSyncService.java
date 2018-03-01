package org.wikipedia.savedpages;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.okhttp.cache.DiskLruCacheUtil;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
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
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.CacheControl;
import okhttp3.CacheDelegate;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.cache.DiskLruCache;
import retrofit2.Call;

import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.SAVE_CACHE;

public class SavedPageSyncService extends JobIntentService {
    // Unique job ID for this service (do not duplicate).
    private static final int JOB_ID = 1000;
    private static final int ENQUEUE_DELAY_MILLIS = 2000;
    private static Runnable ENQUEUE_RUNNABLE = () -> enqueueWork(WikipediaApp.getInstance(),
            SavedPageSyncService.class, JOB_ID, new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));

    @NonNull private final CacheDelegate cacheDelegate = new CacheDelegate(SAVE_CACHE);
    @NonNull private final PageImageUrlParser pageImageUrlParser
            = new PageImageUrlParser(new ImageTagParser(), new PixelDensityDescriptorParser());
    private long blockSize;
    private SavedPageSyncNotification savedPageSyncNotification;

    public SavedPageSyncService() {
        blockSize = FileUtil.blockSize(cacheDelegate.diskLruCache().getDirectory());
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

        List<ReadingListPage> pagesToSave = ReadingListDbHelper.instance().getAllPagesToBeSaved();
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
        Call<PageLead> leadCall = reqPageLead(CacheControl.FORCE_CACHE, pageTitle);
        try {
            lead = leadCall.execute().body();
        } catch (IOException ignore) { }

        if (lead != null) {
            for (String url : pageImageUrlParser.parse(lead)) {
                cacheDelegate.remove(saveImageReq(pageTitle.getWikiSite(), url));
            }
            cacheDelegate.remove(leadCall.request());
        }

        Call<PageRemaining> sectionsCall = reqPageSections(CacheControl.FORCE_CACHE, pageTitle);
        PageRemaining sections = null;
        try {
            sections = sectionsCall.execute().body();
        } catch (IOException ignore) { }

        if (sections != null) {
            for (String url : pageImageUrlParser.parse(sections)) {
                cacheDelegate.remove(saveImageReq(pageTitle.getWikiSite(), url));
            }
            cacheDelegate.remove(sectionsCall.request());
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
                ReadingListDbHelper.instance().markPagesForOffline(queue, false);
                break;
            }
            savedPageSyncNotification.setNotificationProgress(getApplicationContext(), itemsTotal, itemsSaved);

            boolean success = false;
            @Nullable AggregatedResponseSize size = null;
            try {

                // Lengthy operation during which the db state may change...
                size = savePageFor(page);
                success = true;

            } catch (InterruptedException e) {
                // fall through
            } catch (Exception e) {
                // This can be an IOException from the storage media, or several types
                // of network exceptions from malformed URLs, timeouts, etc.
                e.printStackTrace();
                if (!ThrowableUtil.isOffline(e) && !ThrowableUtil.is404(e)) {
                    // If it's anything but a transient network error, let's log it aggressively,
                    // to make sure we've fixed any other errors with saving pages.
                    L.logRemoteError(e);
                }
            }

            if (ReadingListSyncAdapter.inProgress()) {
                // Reading list sync was started in the meantime, so bail.
                break;
            }

            if (success) {
                page.status(ReadingListPage.STATUS_SAVED);
                page.sizeBytes(size.logicalSize());
                ReadingListDbHelper.instance().updatePage(page);
                itemsSaved++;
                sendSyncEvent();
            }
        }
        return itemsSaved;
    }

    @NonNull private AggregatedResponseSize savePageFor(@NonNull ReadingListPage page) throws IOException, InterruptedException {
        PageTitle pageTitle = ReadingListPage.toPageTitle(page);
        AggregatedResponseSize size = new AggregatedResponseSize(0, 0, 0);

        Call<PageLead> leadCall = reqPageLead(null, pageTitle);
        Call<PageRemaining> sectionsCall = reqPageSections(null, pageTitle);

        retrofit2.Response<PageLead> leadRsp = leadCall.execute();
        size = size.add(responseSize(leadRsp));
        retrofit2.Response<PageRemaining> sectionsRsp = sectionsCall.execute();
        size = size.add(responseSize(sectionsRsp));

        if (!TextUtils.isEmpty(leadRsp.body().getThumbUrl())) {
            persistPageThumbnail(pageTitle, leadRsp.body().getThumbUrl());
            page.thumbUrl(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                    leadRsp.body().getThumbUrl()));
        }
        page.description(leadRsp.body().getDescription());

        Set<String> imageUrls = new HashSet<>(pageImageUrlParser.parse(leadRsp.body()));
        imageUrls.addAll(pageImageUrlParser.parse(sectionsRsp.body()));

        if (Prefs.isImageDownloadEnabled()) {
            size = size.add(reqSaveImage(pageTitle.getWikiSite(), imageUrls));
        }

        String title = pageTitle.getPrefixedText();
        L.i("Saved page " + title + " (" + size + ")");

        return size;
    }

    @NonNull private Call<PageLead> reqPageLead(@Nullable CacheControl cacheControl,
                                                @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        int thumbnailWidth = DimenUtil.calculateLeadImageWidth();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.lead(cacheControl, cacheOption, title, thumbnailWidth);
    }

    @NonNull private Call<PageRemaining> reqPageSections(@Nullable CacheControl cacheControl,
                                                         @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.sections(cacheControl, cacheOption, title);
    }

    private AggregatedResponseSize reqSaveImage(@NonNull WikiSite wiki, @NonNull Iterable<String> urls) throws IOException, InterruptedException {
        AggregatedResponseSize size = new AggregatedResponseSize(0, 0, 0);
        for (String url : urls) {
            if (savedPageSyncNotification.isSyncPaused() || savedPageSyncNotification.isSyncCanceled()) {
                throw new InterruptedException("Sync paused or cancelled.");
            }
            try {
                size = size.add(reqSaveImage(wiki, url));
            } catch (Exception e) {
                if (isRetryable(e)) {
                    throw e;
                }
            }
        }
        return size;
    }

    @NonNull private ResponseSize reqSaveImage(@NonNull WikiSite wiki, @NonNull String url) throws IOException {
        Request request = saveImageReq(wiki, url);

        Response rsp = OkHttpConnectionFactory.getClient().newCall(request).execute();

        // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is read
        // for the cache to be written.
        rsp.body().close();

        // Size must be checked after the body has been written.
        return responseSize(rsp);
    }

    @NonNull private Request saveImageReq(@NonNull WikiSite wiki, @NonNull String url) {
        return new Request
                .Builder()
                .addHeader(SaveHeader.FIELD, SaveHeader.VAL_ENABLED)
                .url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
                .build();
    }

    private void persistPageThumbnail(@NonNull PageTitle title, @NonNull String url) {
        WikipediaApp.getInstance().getDatabaseClient(PageImage.class).upsert(
                new PageImage(title, UriUtil.resolveProtocolRelativeUrl(title.getWikiSite(), url)),
                PageImageHistoryContract.Image.SELECTION);
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

    @NonNull private ResponseSize responseSize(@NonNull Response rsp) {
        return responseSize(rsp.request());
    }

    @NonNull private ResponseSize responseSize(@NonNull retrofit2.Response rsp) {
        return responseSize(rsp.raw().request());
    }

    @NonNull private ResponseSize responseSize(@NonNull Request req) {
        return responseSize(cacheDelegate.entry(req));
    }

    @NonNull private ResponseSize responseSize(@Nullable DiskLruCache.Snapshot snapshot) {
        long metadataSize = DiskLruCacheUtil.okHttpResponseMetadataSize(snapshot);
        long bodySize = DiskLruCacheUtil.okHttpResponseBodySize(snapshot);
        return new ResponseSize(metadataSize, bodySize);
    }

    @NonNull private PageClient newPageClient(@NonNull PageTitle title) {
        return PageClientFactory.create(title.getWikiSite(), title.namespace());
    }

    private static class AggregatedResponseSize {
        private final long physicalSize;
        private final long logicalSize;
        private final int responsesAggregated;

        AggregatedResponseSize(long physicalSize, long logicalSize, int responsesAggregated) {
            this.physicalSize = physicalSize;
            this.logicalSize = logicalSize;
            this.responsesAggregated = responsesAggregated;
        }

        @Override public String toString() {
            return "responses=" + responsesAggregated() + " physical=" + physicalSize() + "B logical=" + logicalSize() + "B";
        }

        long physicalSize() {
            return physicalSize;
        }

        // The size on disk.
        long logicalSize() {
            return logicalSize;
        }

        int responsesAggregated() {
            return responsesAggregated;
        }

        @NonNull AggregatedResponseSize add(@NonNull ResponseSize size) {
            return new AggregatedResponseSize(physicalSize + size.physicalSize(),
                    logicalSize + size.logicalSize(), responsesAggregated() + 1);
        }

        @NonNull AggregatedResponseSize add(@NonNull AggregatedResponseSize size) {
            return new AggregatedResponseSize(physicalSize + size.physicalSize(),
                    logicalSize + size.logicalSize(), responsesAggregated() + size.responsesAggregated());
        }
    }

    private class ResponseSize {
        private final long metadataSize;
        private final long bodySize;

        ResponseSize(long metadataSize, long bodySize) {
            this.metadataSize = metadataSize;
            this.bodySize = bodySize;
        }

        @Override public String toString() {
            return "physical metadata=" + metadataSize + "B physical body=" + bodySize
                    + "B physical=" + physicalSize() + "B logical=" + logicalSize() + "B";
        }

        long physicalSize() {
            return metadataSize + bodySize;
        }

        long logicalSize() {
            return FileUtil.physicalToLogicalSize(metadataSize, blockSize)
                    + FileUtil.physicalToLogicalSize(bodySize, blockSize);
        }
     }
}
