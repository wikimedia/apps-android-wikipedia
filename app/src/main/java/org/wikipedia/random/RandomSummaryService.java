package org.wikipedia.random;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

public class RandomSummaryService {
    @NonNull private final RbCachedService<RbRandomSummaryClient> cachedService
            = new RbCachedService<>(RbRandomSummaryClient.class);
    @NonNull private final WikipediaZeroHandler responseHeaderHandler;
    @NonNull private Site site;
    @NonNull private RandomSummaryCallback cb;

    public RandomSummaryService(@NonNull Site site,
                                @NonNull RandomSummaryCallback cb) {
        this.responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        this.site = site;
        this.cb = cb;
    }

    public void get() {
        Call<CardPageItem> call = cachedService.service(site).get();
        call.enqueue(new Callback<CardPageItem>() {
            @Override
            public void onResponse(Call<CardPageItem> call, Response<CardPageItem> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    CardPageItem item = response.body();
                    String namespace = item.namespace() == null ? null
                            : item.namespace().toLegacyString();
                    PageTitle title = new PageTitle(namespace, item.title(), site);
                    cb.onSuccess(title);
                } else {
                    L.v(response.message());
                    cb.onError(new IOException(response.message()));
                }
            }

            @Override
            public void onFailure(Call<CardPageItem> call, Throwable t) {
                L.w("Failed to get random page title/summary", t);
                cb.onError(t);
            }
        });
    }

    private interface RbRandomSummaryClient {
        @GET("page/random/summary")
        @NonNull Call<CardPageItem> get();
    }


    public interface RandomSummaryCallback {
        void onSuccess(PageTitle title);
        void onError(Throwable t);
    }
}
