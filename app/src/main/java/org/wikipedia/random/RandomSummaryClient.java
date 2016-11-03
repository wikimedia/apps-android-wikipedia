package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class RandomSummaryClient {
    @NonNull private final RbCachedService<Service> cachedService
            = new RbCachedService<>(Service.class);
    @NonNull private final WikipediaZeroHandler responseHeaderHandler;
    @NonNull private WikiSite wiki;
    @NonNull private Callback cb;

    public RandomSummaryClient(@NonNull WikiSite wiki, @NonNull Callback cb) {
        this.responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        this.wiki = wiki;
        this.cb = cb;
    }

    public void request() {
        Call<CardPageItem> call = cachedService.service(wiki).get();
        call.enqueue(new retrofit2.Callback<CardPageItem>() {
            @Override
            public void onResponse(@NonNull Call<CardPageItem> call,
                                   @NonNull Response<CardPageItem> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    CardPageItem item = response.body();
                    String namespace = item.namespace().toLegacyString();
                    PageTitle title = new PageTitle(namespace, item.title(), wiki);
                    cb.onSuccess(call, title);
                } else {
                    L.v(response.message());
                    cb.onError(call, new IOException(response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CardPageItem> call, @NonNull Throwable t) {
                L.w("Failed to get random page title/summary", t);
                cb.onError(call, t);
            }
        });
    }

    private interface Service {
        @GET("page/random/summary")
        @NonNull Call<CardPageItem> get();
    }


    public interface Callback {
        void onSuccess(@NonNull Call<CardPageItem> call, @Nullable PageTitle title);
        void onError(@NonNull Call<CardPageItem> call, @NonNull Throwable t);
    }
}
