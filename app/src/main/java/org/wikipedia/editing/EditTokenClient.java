package org.wikipedia.editing;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class EditTokenClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @NonNull public Call<EditToken> request(@NonNull final WikiSite wiki,
                                            @NonNull final Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, cb);
    }

    @VisibleForTesting @NonNull Call<EditToken> request(@NonNull final Service service,
                                                        @NonNull final Callback cb) {
        Call<EditToken> call = service.editToken();
        call.enqueue(new retrofit2.Callback<EditToken>() {
            @Override
            public void onResponse(Call<EditToken> call, Response<EditToken> response) {
                if (response.isSuccessful()) {
                    cb.success(call, response.body().token());
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
                }
            }

            @Override
            public void onFailure(Call<EditToken> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<EditToken> call, @NonNull String token);
        void failure(@NonNull Call<EditToken> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&meta=tokens&type=csrf")
        Call<EditToken> editToken();
    }
}
