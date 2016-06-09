package org.wikipedia.readinglist;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.mediawiki.api.json.Api;
import org.wikipedia.Constants;
import org.wikipedia.Site;
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
        Map<Site, List<PageTitle>> titlesPerSite = new HashMap<>();
        for (ReadingListPage page : readingList.getPages()) {
            if (TextUtils.isEmpty(page.thumbnailUrl()) || TextUtils.isEmpty(page.description())) {
                PageTitle title = ReadingListDaoProxy.pageTitle(page);
                Site siteKey = null;
                for (Site site : titlesPerSite.keySet()) {
                    if (site.equals(title.getSite())) {
                        siteKey = site;
                        break;
                    }
                }
                if (siteKey == null) {
                    siteKey = title.getSite();
                    titlesPerSite.put(siteKey, new ArrayList<PageTitle>());
                }
                titlesPerSite.get(siteKey).add(title);
            }
        }

        for (Site site : titlesPerSite.keySet()) {
            getThumbnailsForTitles(readingList, titlesPerSite.get(site), listener);
        }
    }

    private static void getThumbnailsForTitles(final ReadingList readingList,
                                               @NonNull final List<PageTitle> titles,
                                               @NonNull final CompleteListener listener) {
        Site site = titles.get(0).getSite();
        Api api = WikipediaApp.getInstance().getAPIForSite(titles.get(0).getSite());
        new ReadingListPageInfoTask(api, site, titles, Constants.PREFERRED_THUMB_SIZE) {
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
