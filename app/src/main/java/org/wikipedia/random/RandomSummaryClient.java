package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import static org.wikipedia.Constants.ACCEPT_HEADER_SUMMARY;

public class RandomSummaryClient {
    @NonNull private final WikiCachedService<Service> cachedService
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
                if (response.body() == null) {
                    cb.onError(call, new JsonParseException("Response missing required field(s)"));
                    return;
                }
                RbPageSummary item = response.body();
                PageTitle title = new PageTitle(null, item.getTitle(), wiki);
                cb.onSuccess(call, title);
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
        @Headers(ACCEPT_HEADER_SUMMARY)
        @GET("page/random/summary")
        @NonNull Call<RbPageSummary> get();
    }

    public interface Callback {
        void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull PageTitle title);
        void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t);
    }
}
