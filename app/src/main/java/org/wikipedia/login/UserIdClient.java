package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class UserIdClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, int userId);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(cachedService.service(wiki), cb);
    }

    public Call<MwQueryResponse> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.request();
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    int userId = response.body().query().userInfo().id();
                    cb.success(call, userId);
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable caught) {
                cb.failure(call, caught);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&meta=userinfo")
        @NonNull Call<MwQueryResponse> request();
    }
}
