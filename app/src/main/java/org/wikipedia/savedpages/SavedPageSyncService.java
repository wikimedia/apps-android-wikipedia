package org.wikipedia.savedpages;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.html.ImageTagParser;
import org.wikipedia.html.PixelDensityDescriptorParser;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import okhttp3.CacheControl;
import okhttp3.CacheDelegate;
import okhttp3.Request;
import retrofit2.Call;

import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.SAVE_CACHE;

public class SavedPageSyncService extends IntentService {
    @NonNull private ReadingListPageDao dao;
    @NonNull private final CacheDelegate cacheDelegate = new CacheDelegate(SAVE_CACHE);
    @NonNull private final PageImageUrlParser pageImageUrlParser
            = new PageImageUrlParser(new ImageTagParser(), new PixelDensityDescriptorParser());

    public SavedPageSyncService() {
        super("SavedPageSyncService");
        dao = ReadingListPageDao.instance();
    }

    @Override protected void onHandleIntent(@Nullable Intent intent) {
        // todo: allow deletes while offline
        if (!DeviceUtil.isOnline(this)) {
            L.i("Device is offline; aborting sync service");
            return;
        }

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
        }
        dao.completeDiskTransaction(row);
    }

    private void saveNewEntries(List<ReadingListPageDiskRow> queue) {
        while (!queue.isEmpty()) {
            ReadingListPageDiskRow row = queue.get(0);
            boolean ok = savePageFor(row);
            if (!ok) {
                dao.failDiskTransaction(queue);
                break;
            }
            dao.completeDiskTransaction(row);
            queue.remove(row);
        }
    }

    private boolean savePageFor(@NonNull ReadingListPageDiskRow row) {
        PageTitle pageTitle = makeTitleFrom(row);
        if (pageTitle == null) {
            return false;
        }

        String title = pageTitle.getPrefixedText();
        ImmutablePair<PageLead, PageRemaining> page;
        try {
            page = reqPage(null, pageTitle);
            reqSaveImage(pageTitle.getWikiSite(), pageImageUrlParser.parse(page.getLeft()));
            reqSaveImage(pageTitle.getWikiSite(), pageImageUrlParser.parse(page.getRight()));
        } catch (IOException e) {
            L.e("Failed to save page " + title, e);
            return false;
        }
        L.i("Saved page " + title);
        return true;
    }

    @NonNull private ImmutablePair<PageLead, PageRemaining> reqPage(@Nullable CacheControl cacheControl,
                                                                    @NonNull PageTitle pageTitle) throws IOException {
        PageLead lead = reqPageLead(cacheControl, pageTitle).execute().body();
        PageRemaining sections = reqPageSections(cacheControl, pageTitle).execute().body();
        return new ImmutablePair<>(lead, sections);
    }

    @NonNull private Call<PageLead> reqPageLead(@Nullable CacheControl cacheControl,
                                                @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        int thumbnailWidth = DimenUtil.calculateLeadImageWidth();
        boolean noImages = !WikipediaApp.getInstance().isImageDownloadEnabled();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.lead(cacheControl, cacheOption, title, thumbnailWidth, noImages);
    }

    @NonNull private Call<PageRemaining> reqPageSections(@Nullable CacheControl cacheControl,
                                                         @NonNull PageTitle pageTitle) {
        PageClient client = newPageClient(pageTitle);

        String title = pageTitle.getPrefixedText();
        boolean noImages = !WikipediaApp.getInstance().isImageDownloadEnabled();
        PageClient.CacheOption cacheOption = PageClient.CacheOption.SAVE;

        return client.sections(cacheControl, cacheOption, title, noImages);
    }

    private void reqSaveImage(@NonNull WikiSite wiki, @NonNull List<String> urls) throws IOException {
        for (String url : urls) {
            reqSaveImage(wiki, url);
        }
    }

    private void reqSaveImage(@NonNull WikiSite wiki, @NonNull String url) throws IOException {
        Request request = saveImageReq(wiki, url);

        // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is read
        // for the cache to be written.
        OkHttpConnectionFactory.getClient().newCall(request).execute().body().close();
    }

    @NonNull private Request saveImageReq(@NonNull WikiSite wiki, @NonNull String url) {
        return new Request
                .Builder()
                .addHeader(SaveHeader.FIELD, SaveHeader.VAL_ENABLED)
                .url(UriUtil.resolveProtocolRelativeUrl(wiki, url))
                .build();
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
}
