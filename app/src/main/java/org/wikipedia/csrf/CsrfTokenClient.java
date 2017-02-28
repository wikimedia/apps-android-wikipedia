package org.wikipedia.csrf;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class CsrfTokenClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @NonNull public Call<MwQueryResponse<CsrfToken>> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, cb);
    }

    @VisibleForTesting @NonNull Call<MwQueryResponse<CsrfToken>> request(@NonNull Service service,
                                                                         @NonNull final Callback cb) {
        Call<MwQueryResponse<CsrfToken>> call = service.request();
        call.enqueue(new retrofit2.Callback<MwQueryResponse<CsrfToken>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<CsrfToken>> call, Response<MwQueryResponse<CsrfToken>> response) {
                if (response.isSuccessful()) {
                    if (response.body().success()) {
                        // noinspection ConstantConditions
                        cb.success(call, response.body().query().token());
                    } else if (response.body().hasError()) {
                        // noinspection ConstantConditions
                        cb.failure(call, new MwException(response.body().getError()));
                    } else {
                        cb.failure(call, new IOException("An unknown error occurred."));
                    }
                } else {
                    cb.failure(call, RetrofitException.httpError(response));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<CsrfToken>> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<CsrfToken>> call, @NonNull String token);
        void failure(@NonNull Call<MwQueryResponse<CsrfToken>> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&meta=tokens&type=csrf")
        Call<MwQueryResponse<CsrfToken>> request();
    }
}
