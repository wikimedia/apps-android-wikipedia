package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class RandomSummaryClient {
    @NonNull private final RbCachedService<Service> cachedService
            = new RbCachedService<>(Service.class);

    public Call<RbPageSummary> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(cachedService.service(wiki), wiki, cb);
    }

    @VisibleForTesting Call<RbPageSummary> request(@NonNull Service service,
                                                   @NonNull final WikiSite wiki,
                                                   @NonNull final Callback cb) {
        Call<RbPageSummary> call = service.get();
        call.enqueue(new retrofit2.Callback<RbPageSummary>() {
            @Override
            public void onResponse(@NonNull Call<RbPageSummary> call,
                                   @NonNull Response<RbPageSummary> response) {
                if (response.isSuccessful()) {
                    if (response.body() == null) {
                        cb.onError(call, new JsonParseException("Response missing required field(s)"));
                        return;
                    }
                    WikipediaApp.getInstance().getWikipediaZeroHandler().onHeaderCheck(response);
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
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("page/random/summary")
        @NonNull Call<RbPageSummary> get();
    }

    public interface Callback {
        void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull PageTitle title);
        void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t);
    }
}
