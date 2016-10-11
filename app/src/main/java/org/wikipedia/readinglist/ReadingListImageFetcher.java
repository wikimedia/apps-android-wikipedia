package org.wikipedia.readinglist;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.mediawiki.api.json.Api;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReadingListImageFetcher {

    public interface CompleteListener {
        void onComplete();
        void onError(Throwable e);
    }

    public static void getThumbnails(final ReadingList readingList,
                                     @NonNull final CompleteListener listener) {
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
            getThumbnailsForTitles(readingList, titlesPerSite.get(wiki), listener);
        }
    }

    private static void getThumbnailsForTitles(final ReadingList readingList,
                                               @NonNull final List<PageTitle> titles,
                                               @NonNull final CompleteListener listener) {
        WikiSite wiki = titles.get(0).getWikiSite();
        Api api = WikipediaApp.getInstance().getAPIForSite(titles.get(0).getWikiSite());
        new ReadingListPageInfoTask(api, wiki, titles, Constants.PREFERRED_THUMB_SIZE) {
            @Override
            public void onFinish(Map<PageTitle, Void> result) {
                for (PageTitle title : titles) {
                    if (result.containsKey(title)) {
                        for (ReadingListPage page : readingList.getPages()) {
                            if (title.getDisplayText().equals(page.title())) {
                                page.setThumbnailUrl(title.getThumbUrl());
                                page.setDescription(title.getDescription());
                                ReadingListPageDao.instance().upsert(page);
                            }
                        }
                    }
                }
                listener.onComplete();
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w(caught);
                listener.onError(caught);
            }
        }.execute();
    }

    private ReadingListImageFetcher() {
    }
}
