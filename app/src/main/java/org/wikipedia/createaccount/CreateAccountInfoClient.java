package org.wikipedia.createaccount;

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

class CreateAccountInfoClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<CreateAccountInfo>> call, @NonNull CreateAccountInfoResult result);
        void failure(@NonNull Call<MwQueryResponse<CreateAccountInfo>> call, @NonNull Throwable caught);
    }

    Call<MwQueryResponse<CreateAccountInfo>> request(@NonNull WikiSite wiki,
                                                     @NonNull Callback cb) {
        return request(cachedService.service(wiki), cb);
    }

    @VisibleForTesting Call<MwQueryResponse<CreateAccountInfo>> request(@NonNull Service service,
                                                                        @NonNull final Callback cb) {
        Call<MwQueryResponse<CreateAccountInfo>> call = service.request();
        call.enqueue(new retrofit2.Callback<MwQueryResponse<CreateAccountInfo>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<CreateAccountInfo>> call,
                                   Response<MwQueryResponse<CreateAccountInfo>> response) {
                if (response.isSuccessful()) {
                    if (response.body().success()) {
                        String token = response.body().query().token();
                        String captchaId = response.body().query().captchaId();
                        cb.success(call, new CreateAccountInfoResult(token, captchaId));
                    } else if (response.body().hasError()) {
                        cb.failure(call, new MwException(response.body().getError()));
                    } else {
                        cb.failure(call, new IOException("An unknown error occurred."));
                    }
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<CreateAccountInfo>> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
        Call<MwQueryResponse<CreateAccountInfo>> request();
    }
}
