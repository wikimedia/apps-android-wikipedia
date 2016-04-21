package org.wikipedia.readinglist;

import android.support.annotation.NonNull;

import org.mediawiki.api.json.Api;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.util.log.L;

import java.util.List;
import java.util.Map;

public final class ReadingListImageFetcher {

    public interface CompleteListener {
        void onComplete();
        void onError(Throwable e);
    }

    // TODO: clean up, and make interwiki
    public static void getThumbnails(final ReadingList readingList, @NonNull final CompleteListener listener) {
        if (readingList.getPages().isEmpty()) {
            return;
        }
        Site site = readingList.getPages().get(0).site();
        Api api = WikipediaApp.getInstance().getAPIForSite(site);
        List<PageTitle> titles = ReadingListDaoProxy.pageTitles(readingList.getPages());
        new PageImagesTask(api, site, titles, WikipediaApp.PREFERRED_THUMB_SIZE) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                for (ReadingListPage page : readingList.getPages()) {
                    PageTitle title = ReadingListDaoProxy.pageTitle(page);
                    if (result.containsKey(title)) {
                        // update this thumbnail in the db?
                        //PageImage pi = new PageImage(model.getTitle(), result.get(model.getTitle()));
                        //app.getDatabaseClient(PageImage.class).upsert(pi, PageImageDatabaseTable.Col.SELECTION);
                        //page.setThumbUrl(result.get(title));
                        ReadingListPage copy = ReadingListPage
                            .builder()
                            .copy(page)
                            .thumbnailUrl(result.get(title))
                            .build();
                        ReadingListPageDao.instance().upsertAsync(copy);
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
