package org.wikipedia.descriptions.centralauth;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.POST;

/**
 * When accessing the API using a cross-domain AJAX request (CORS), use this to authenticate as the
 * current SUL user. Use action=centralauthtoken on this wiki to retrieve the token, before making
 * the CORS request. Each token may only be used once, and expires after 10 seconds.
 */
public class CentralAuthTokenClient {
    @NonNull
    private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<CentralAuthToken> call, @NonNull String token);
        void failure(@NonNull Call<CentralAuthToken> call, @NonNull Throwable caught);
    }

    @NonNull public Call<CentralAuthToken> request(@NonNull final WikiSite wiki,
                                                   @NonNull final Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, cb);
    }

    @VisibleForTesting @NonNull Call<CentralAuthToken> request(@NonNull final Service service,
                                                               @NonNull final Callback cb) {
        Call<CentralAuthToken> call = service.get();
        call.enqueue(new retrofit2.Callback<CentralAuthToken>() {
            @Override
            public void onResponse(Call<CentralAuthToken> call, Response<CentralAuthToken> response) {
                if (response.isSuccessful()) {
                    final CentralAuthToken body = response.body();
                    if (body.success()) {
                        cb.success(call, body.getToken());
                    } else if (body.hasError()) {
                        cb.failure(call, new CentralAuthTokenRetrievalFailedException(body.getError()));
                    } else {
                        // no error and no token. Whaaat?
                        cb.failure(call, new RuntimeException("unexpected response from server"));
                    }
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
                }
            }

            @Override
            public void onFailure(Call<CentralAuthToken> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @POST("w/api.php?action=centralauthtoken&format=json")
        Call<CentralAuthToken> get();
    }
}
