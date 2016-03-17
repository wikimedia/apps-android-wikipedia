package org.wikipedia.readinglist;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.util.log.L;

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
        Site site = readingList.getPages().get(0).getSite();
        (new PageImagesTask(WikipediaApp.getInstance().getAPIForSite(site), site, readingList.getPages(), WikipediaApp.PREFERRED_THUMB_SIZE) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                for (PageTitle title : readingList.getPages()) {
                    if (result.containsKey(title)) {
                        // update this thumbnail in the db?
                        //PageImage pi = new PageImage(model.getTitle(), result.get(model.getTitle()));
                        //app.getDatabaseClient(PageImage.class).upsert(pi, PageImageDatabaseTable.Col.SELECTION);
                        title.setThumbUrl(result.get(title));
                    }
                }
                listener.onComplete();
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w(caught);
                listener.onError(caught);
            }
        }).execute();
    }

    private ReadingListImageFetcher() {
    }
}
