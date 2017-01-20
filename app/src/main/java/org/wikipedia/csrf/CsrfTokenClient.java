package org.wikipedia.csrf;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class CsrfTokenClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @NonNull public Call<CsrfToken> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, cb);
    }

    @VisibleForTesting @NonNull Call<CsrfToken> request(@NonNull Service service,
                                                        @NonNull final Callback cb) {
        Call<CsrfToken> call = service.request();
        call.enqueue(new retrofit2.Callback<CsrfToken>() {
            @Override
            public void onResponse(Call<CsrfToken> call, Response<CsrfToken> response) {
                if (response.isSuccessful()) {
                    cb.success(call, response.body().token());
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
                }
            }

            @Override
            public void onFailure(Call<CsrfToken> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<CsrfToken> call, @NonNull String token);
        void failure(@NonNull Call<CsrfToken> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&meta=tokens&type=csrf")
        Call<CsrfToken> request();
    }
}
