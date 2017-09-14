package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.util.log.L;

import retrofit2.Call;

public final class RandomArticleRequestHandler{
    public interface Callback {
        void onSuccess(@NonNull PageTitle pageTitle);
        void onError(Throwable t);
    }

    public static void getRandomPage(@NonNull final Callback cb) {
        new RandomSummaryClient().request(WikipediaApp.getInstance().getWikiSite(), new RandomSummaryClient.Callback() {
            @Override
            public void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull PageTitle title) {
                cb.onSuccess(title);
            }

            @Override
            public void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                L.w("Failed to get random card from network. Falling back to compilations.", t);
                if (OfflineManager.hasCompilation()) {
                    getRandomPageFromCompilation(cb);
                } else {
                    getRandomPageFromReadingLists(cb, t);
                }
            }
        });
    }

    private static void getRandomPageFromCompilation(@NonNull Callback cb) {
        try {
            cb.onSuccess(new PageTitle(OfflineManager.instance().getRandomTitle(), WikipediaApp.getInstance().getWikiSite()));
        } catch (Throwable t) {
            cb.onError(t);
        }
    }

    private static void getRandomPageFromReadingLists(@NonNull final Callback cb, @NonNull final Throwable throwableIfEmpty) {
        ReadingListPageDao.instance().randomPage(new CallbackTask.DefaultCallback<PageTitle>() {
            @Override
            public void success(@Nullable PageTitle title) {
                if (title != null) {
                    cb.onSuccess(title);
                } else {
                    cb.onError(throwableIfEmpty);
                }
            }
        });
    }


    private RandomArticleRequestHandler() { }
}
