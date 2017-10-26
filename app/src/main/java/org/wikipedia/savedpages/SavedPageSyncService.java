package org.wikipedia.savedpages;

import android.content.Context;
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
import org.wikipedia.readinglist.ReadingListData;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class SavedPageSyncService extends JobIntentService {
    // Unique job ID for this service (do not duplicate).
    private static final int JOB_ID = 1000;

    @NonNull private ReadingListPageDao dao;
    @NonNull private final CacheDelegate cacheDelegate = new CacheDelegate(SAVE_CACHE);
    @NonNull private final PageImageUrlParser pageImageUrlParser
            = new PageImageUrlParser(new ImageTagParser(), new PixelDensityDescriptorParser());
    private long blockSize;

    public SavedPageSyncService() {
        dao = ReadingListPageDao.instance();
        blockSize = FileUtil.blockSize(cacheDelegate.diskLruCache().getDirectory());
    }

    public static void enqueueService(@NonNull Context context) {
        enqueueWork(context, SavedPageSyncService.class, JOB_ID,
                new Intent(context, SavedPageSyncService.class));
    }

    @Override protected void onHandleWork(@NonNull Intent intent) {
        List<ReadingListPageDiskRow> queue = new ArrayList<>();
        Collection<ReadingListPageDiskRow> rows = dao.startDiskTransaction();

        for (ReadingListPageDiskRow row : rows) {
            switch (row.status()) {
                case UNSAVED:
                case DELETED:
                    deleteRow(row);
                    break;
                case OUTDATED:
                    queue.add(row);
                    break;
                case ONLINE:
                case SAVED:
                    // SavedPageSyncService observes all list changes. No transaction is pending
                    // when the row is online or saved.
                    break;
                default:
                    throw new UnsupportedOperationException("Invalid disk row status: "
                            + row.status().name());
            }
        }
        saveNewEntries(queue);
    }

    private void sendSyncEvent() {
        // Note: this method posts from a background thread but subscribers expect events to be
        // received on the main thread.
        WikipediaApp.getInstance().getBus().post(new ReadingListSyncEvent());
    }

    private void deleteRow(@NonNull ReadingListPageDiskRow row) {
        ReadingListPageRow dat = row.dat();
        PageTitle pageTitle = makeTitleFrom(row);
        if (dat != null && pageTitle != null) {
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
            ReadingListPageDao.instance().deleteIfOrphaned(dat);
        }
        dao.completeDiskTransaction(row);
    }

    private void saveNewEntries(List<ReadingListPageDiskRow> queue) {
        sendSyncEvent();
        while (!queue.isEmpty()) {

            // Pick off the DB row that we'll be working on...
            ReadingListPageDiskRow tempRow = queue.remove(0);

            @Nullable PageTitle pageTitle = makeTitleFrom(tempRow);
            if (pageTitle == null) {
                // TODO: delete this orphaned row from Disk table, since Page item no longer exists.
                dao.failDiskTransaction(tempRow);
                continue;
            }

            boolean success = false;
            @Nullable AggregatedResponseSize size = null;
            try {

                // Lengthy operation during which the db state may change...
                size = savePageFor(tempRow, pageTitle);
                success = true;

            } catch (Exception e) {
                // This can be an IOException from the storage media, or several types
                // of network exceptions from malformed URLs, timeouts, etc.
                e.printStackTrace();
                if (!ThrowableUtil.isOffline(e)) {
                    // If it's anything but a transient network error, let's log it aggressively,
                    // to make sure we've fixed any other errors with saving pages.
                    L.logRemoteError(e);
                }
            }

            // Query the database again, and see if this page title is still actually there,
            // or if the page has been added to more lists.
            @Nullable ReadingListPage page = ReadingListData.instance()
                    .findPageInAnyList(ReadingListDaoProxy.key(pageTitle));
            if (page == null) {
                // The page has been deleted while we were busy!
                continue;
            }

            // Make an updated DiskRow based on the refreshed page.
            ReadingListPageDiskRow updatedRow = new ReadingListPageDiskRow(tempRow,
                    ReadingListPageRow.builder()
                            .copy(page)
                            .logicalSize(size != null ? size.logicalSize() : 0)
                            .physicalSize(size != null ? size.physicalSize() : 0)
                            .build());

            if (success) {
                dao.completeDiskTransaction(updatedRow);
                sendSyncEvent();
            } else {
                dao.failDiskTransaction(updatedRow);
            }
        }
    }

    @NonNull private AggregatedResponseSize savePageFor(@NonNull ReadingListPageDiskRow row,
                                                        @NonNull PageTitle pageTitle) throws IOException {
        AggregatedResponseSize size = new AggregatedResponseSize(0, 0, 0);

        Call<PageLead> leadCall = reqPageLead(null, pageTitle);
        Call<PageRemaining> sectionsCall = reqPageSections(null, pageTitle);

        retrofit2.Response<PageLead> leadRsp = leadCall.execute();
        size = size.add(responseSize(leadRsp));
        retrofit2.Response<PageRemaining> sectionsRsp = sectionsCall.execute();
        size = size.add(responseSize(sectionsRsp));

        if (!TextUtils.isEmpty(leadRsp.body().getThumbUrl())) {
            persistPageThumbnail(pageTitle, leadRsp.body().getThumbUrl());
            row.dat().setThumbnailUrl(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                    leadRsp.body().getThumbUrl()));
        }
        row.dat().setDescription(leadRsp.body().getDescription());

        Set<String> imageUrls = new HashSet<>(pageImageUrlParser.parse(leadRsp.body()));
        imageUrls.addAll(pageImageUrlParser.parse(sectionsRsp.body()));

        size = size.add(reqSaveImage(pageTitle.getWikiSite(), imageUrls));

        String title = pageTitle.getPrefixedText();
        L.i("Saved page " + title + " (" + size + ")");

        return size;
    }

    @NonNull private Call<PageLead> reqPageLead(@Nullable CacheControl cacheControl,
                                                @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        int thumbnailWidth = DimenUtil.calculateLeadImageWidth();
        boolean noImages = !isImageDownloadEnabled();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.lead(cacheControl, cacheOption, title, thumbnailWidth, noImages);
    }

    @NonNull private Call<PageRemaining> reqPageSections(@Nullable CacheControl cacheControl,
                                                         @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        boolean noImages = !isImageDownloadEnabled();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.sections(cacheControl, cacheOption, title, noImages);
    }

    private AggregatedResponseSize reqSaveImage(@NonNull WikiSite wiki, @NonNull Iterable<String> urls) throws IOException {
        AggregatedResponseSize size = new AggregatedResponseSize(0, 0, 0);
        for (String url : urls) {
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
        /*
        "Retryable" in this case refers to exceptions that will be rethrown up to the
        outer exception handler, so that the entire page can be retried on the next pass
        of the sync service.
        Errors that do *not* qualify for retrying include:
        - IllegalArgumentException (thrown for any kind of malformed URL)
        - HTTP 404 status (for nonexistent media)
        */
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

    @Nullable private PageTitle makeTitleFrom(@NonNull ReadingListPageDiskRow row) {
        ReadingListPageRow pageRow = row.dat();
        if (pageRow == null) {
            return null;
        }
        String namespace = pageRow.namespace().toLegacyString();
        return new PageTitle(namespace, pageRow.title(), pageRow.wikiSite());
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
