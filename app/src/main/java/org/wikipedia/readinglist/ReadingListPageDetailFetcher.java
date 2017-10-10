package org.wikipedia.readinglist;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.util.BatchUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

final class ReadingListPageDetailFetcher {
    public interface Callback{
        void success();
        void failure(Throwable caught);
    }

    private static ReadingListPageInfoClient CLIENT = new ReadingListPageInfoClient();

    static void updateInfo(@NonNull ReadingList readingList, @NonNull final Callback cb) {
        Map<WikiSite, List<PageTitle>> titlesPerSite = new HashMap<>();
        for (ReadingListPage page : readingList.getPages()) {
            if (TextUtils.isEmpty(page.thumbnailUrl()) || TextUtils.isEmpty(page.description())) {
                PageTitle title = ReadingListDaoProxy.pageTitle(page);
                WikiSite wikiKey = null;
                for (WikiSite wiki : titlesPerSite.keySet()) {
                    if (wiki.equals(title.getWikiSite())) {
                        wikiKey = wiki;
                        break;
                    }
                }
                if (wikiKey == null) {
                    wikiKey = title.getWikiSite();
                    titlesPerSite.put(wikiKey, new ArrayList<PageTitle>());
                }
                titlesPerSite.get(wikiKey).add(title);
            }
        }

        for (WikiSite wiki : titlesPerSite.keySet()) {
            getInfoForTitles(readingList, titlesPerSite.get(wiki), wiki, cb);
        }
    }

    private static void getInfoForTitles(@NonNull final ReadingList readingList,
                                         @NonNull final List<PageTitle> titles,
                                         @NonNull WikiSite wiki, @NonNull final Callback cb) {
        BatchUtil.makeBatches(titles, new RequestHandler(), new PagesForWikiCallback(readingList, wiki, cb));
    }

    private static class RequestHandler implements BatchUtil.Handler<MwQueryPage> {
        private final List<MwQueryPage> results = new ArrayList<>();

        @Override public void handleBatch(@NonNull final List<PageTitle> titles, final int total,
                                          final BatchUtil.Callback<MwQueryPage> outerCallback) {
            CLIENT.request(titles.get(0).getWikiSite(), titles, new ReadingListPageInfoClient.Callback() {
                @Override public void success(@NonNull Call<MwQueryResponse> call,
                                              @NonNull List<MwQueryPage> queryResults) {
                    results.addAll(queryResults);

                    if (results.size() == total) {
                        outerCallback.success(results);
                    }
                }

                @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                              @NonNull Throwable caught) {
                    L.w(caught);
                    outerCallback.failure(caught);
                }
            });
        }
    }

    private static class PagesForWikiCallback implements BatchUtil.Callback<MwQueryPage> {
        @NonNull private ReadingList readingList;
        @NonNull private WikiSite wiki;
        @NonNull private Callback callback;

        PagesForWikiCallback(@NonNull final ReadingList readingList, @NonNull WikiSite wiki,
                    @NonNull final Callback cb) {
            this.readingList = readingList;
            this.wiki = wiki;
            this.callback = cb;
        }

        @Override public void success(@NonNull List<MwQueryPage> result) {
            Map<String, MwQueryPage> resultMap = makeQueryPageMap(result);
            for (ReadingListPage page : readingList.getPages()) {
                if ((isFromRequestWiki(page)) && resultMap.containsKey(page.title())) {
                    page.setThumbnailUrl(resultMap.get(page.title()).thumbUrl());
                    page.setDescription(resultMap.get(page.title()).description());
                    ReadingListPageDao.instance().upsert(page);
                }
            }
            callback.success();
        }

        @Override public void failure(@NonNull Throwable caught) {
            callback.failure(caught);
            L.w(caught);
        }

        @NonNull private Map<String, MwQueryPage> makeQueryPageMap(@NonNull List<MwQueryPage> pages) {
            Map<String, MwQueryPage> result = new HashMap<>();
            for (MwQueryPage page : pages) {
                result.put(page.title(), page);
                if (!TextUtils.isEmpty(page.convertedFrom())) {
                    result.put(page.convertedFrom(), page);
                }
                if (!TextUtils.isEmpty(page.redirectFrom())) {
                    result.put(page.redirectFrom(), page);
                }
            }
            return result;
        }

        private boolean isFromRequestWiki(@NonNull ReadingListPage page) {
            return wiki.equals(page.wikiSite());
        }
    }

    private ReadingListPageDetailFetcher() {
    }
}
