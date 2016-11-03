package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.restbase.RbPageSummary;
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
        Call<RbPageSummary> call = cachedService.service(wiki).get();
        call.enqueue(new retrofit2.Callback<RbPageSummary>() {
            @Override
            public void onResponse(@NonNull Call<RbPageSummary> call,
                                   @NonNull Response<RbPageSummary> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    RbPageSummary item = response.body();
                    PageTitle title = new PageTitle(null, item.getTitle(), wiki);
                    cb.onSuccess(call, title);
                } else {
                    L.v(response.message());
                    cb.onError(call, new IOException(response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                L.w("Failed to get random page title/summary", t);
                cb.onError(call, t);
            }
        });
    }

    private interface Service {
        @GET("page/random/summary")
        @NonNull Call<RbPageSummary> get();
    }

    public interface Callback {
        void onSuccess(@NonNull Call<RbPageSummary> call, @Nullable PageTitle title);
        void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t);
    }
}
