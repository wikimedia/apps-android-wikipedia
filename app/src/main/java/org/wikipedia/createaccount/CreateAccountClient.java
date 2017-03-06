package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

class CreateAccountClient {
    public interface Callback {
        void success(@NonNull Call<CreateAccountResponse> call, @NonNull CreateAccountSuccessResult result);
        void failure(@NonNull Call<CreateAccountResponse> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @SuppressWarnings("checkstyle:parameternumber")
    Call<CreateAccountResponse> request(@NonNull WikiSite wiki, @NonNull String username,
                                        @NonNull String password, @NonNull String retype,
                                        @NonNull String token, @Nullable String email,
                                        @Nullable String captchaId, @Nullable String captchaWord,
                                        @NonNull Callback cb) {
        return request(cachedService.service(wiki), username, password, retype, token, email, captchaId, captchaWord, cb);
    }

    @SuppressWarnings("checkstyle:parameternumber") @VisibleForTesting
    Call<CreateAccountResponse> request(@NonNull Service service, @NonNull String username,
                                        @NonNull String password, @NonNull String retype,
                                        @NonNull String token, @Nullable String email,
                                        @Nullable String captchaId, @Nullable String captchaWord,
                                        @NonNull final Callback cb) {
        Call<CreateAccountResponse> call = service.request(username, password, retype, token,
                Constants.WIKIPEDIA_URL, email, captchaId, captchaWord);
        call.enqueue(new retrofit2.Callback<CreateAccountResponse>() {
            @Override
            public void onResponse(Call<CreateAccountResponse> call, Response<CreateAccountResponse> response) {
                if (response.body().hasResult()) {
                    CreateAccountResponse result = response.body();
                    if ("PASS".equals(result.status())) {
                        cb.success(call, new CreateAccountSuccessResult(result.user()));
                    } else {
                        cb.failure(call, new CreateAccountException(result.message()));
                    }
                } else if (response.body().hasError()) {
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<CreateAccountResponse> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @SuppressWarnings("checkstyle:parameternumber")
        @FormUrlEncoded
        @POST("w/api.php?action=createaccount&format=json&formatversion=2")
        Call<CreateAccountResponse> request(@NonNull @Field("username") String user,
                                            @NonNull @Field("password") String pass,
                                            @NonNull @Field("retype") String retype,
                                            @NonNull @Field("createtoken") String token,
                                            @NonNull @Field("createreturnurl") String returnurl,
                                            @Nullable @Field("email") String email,
                                            @Nullable @Field("captchaId") String captchaId,
                                            @Nullable @Field("captchaWord") String captchaWord);
    }
}
