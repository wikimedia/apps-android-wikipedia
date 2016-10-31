package org.wikipedia.zero;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * A service to fetch Zero config data.
 *
 * Note: zeroconfig calls still require using the mobile (m-dot) subdomain, which we otherwise no
 * longer use for caching reasons.
 */
class ZeroConfigClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<ZeroConfig> request(@NonNull WikiSite wiki, @NonNull String userAgent,
                                    @NonNull Callback cb) {
        return request(cachedService.service(wiki), userAgent, cb);
    }

    @VisibleForTesting Call<ZeroConfig> request(@NonNull Service service, @NonNull String userAgent,
                                                @NonNull Callback cb) {
        Call<ZeroConfig> call = service.get(userAgent);
        call.enqueue(new CallbackAdapter(cachedService, cb));
        return call;
    }

    interface Callback  {
        void success(@NonNull Call<ZeroConfig> call, @NonNull ZeroConfig config);

        void failure(@NonNull Call<ZeroConfig> call, @NonNull Throwable caught);
    }

    @VisibleForTesting
    public interface Service {
        @GET("w/api.php?action=zeroconfig&format=json&type=message")
        Call<ZeroConfig> get(@NonNull @Query("agent") String userAgent);
    }

    private static class CallbackAdapter implements retrofit2.Callback<ZeroConfig> {
        @NonNull private Callback cb;
        @NonNull private MwCachedService<Service> cachedService;

        CallbackAdapter(@NonNull MwCachedService<Service> service, @NonNull Callback cb) {
            this.cachedService = service;
            this.cb = cb;
        }

        @Override
        public void onResponse(@NonNull Call<ZeroConfig> call,
                               @NonNull Response<ZeroConfig> response) {
            if (response.isSuccessful()) {
                cb.success(call, response.body());
            } else {
                cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
            }
        }

        @Override
        public void onFailure(@NonNull Call<ZeroConfig> call, @NonNull Throwable t) {
            cb.failure(call, t);
        }
    }
}
