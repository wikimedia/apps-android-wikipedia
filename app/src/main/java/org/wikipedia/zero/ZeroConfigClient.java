package org.wikipedia.zero;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;

import retrofit2.Call;
import retrofit2.Response;

/**
 * A service to fetch Zero config data.
 *
 * Note: zeroconfig calls still require using the mobile (m-dot) subdomain, which we otherwise no
 * longer use for caching reasons.
 */
class ZeroConfigClient {
    interface Callback  {
        void success(@NonNull Call<ZeroConfig> call, @NonNull ZeroConfig config);
        void failure(@NonNull Call<ZeroConfig> call, @NonNull Throwable caught);
    }

    public Call<ZeroConfig> request(@NonNull WikiSite wiki, @NonNull String userAgent,
                                    @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), userAgent, cb);
    }

    @VisibleForTesting Call<ZeroConfig> request(@NonNull Service service, @NonNull String userAgent,
                                                @NonNull Callback cb) {
        Call<ZeroConfig> call = service.getZeroConfig(userAgent);
        call.enqueue(new CallbackAdapter(cb));
        return call;
    }

    private static class CallbackAdapter implements retrofit2.Callback<ZeroConfig> {
        @NonNull private Callback cb;

        CallbackAdapter(@NonNull Callback cb) {
            this.cb = cb;
        }

        @Override public void onResponse(@NonNull Call<ZeroConfig> call, @NonNull Response<ZeroConfig> response) {
            cb.success(call, response.body());
        }

        @Override public void onFailure(@NonNull Call<ZeroConfig> call, @NonNull Throwable t) {
            cb.failure(call, t);
        }
    }
}
