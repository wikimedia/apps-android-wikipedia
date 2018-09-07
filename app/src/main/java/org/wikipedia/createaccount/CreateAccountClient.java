package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class CreateAccountClient {
    public interface Callback {
        void success(@NonNull Call<CreateAccountResponse> call, @NonNull CreateAccountSuccessResult result);
        void failure(@NonNull Call<CreateAccountResponse> call, @NonNull Throwable caught);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    Call<CreateAccountResponse> request(@NonNull WikiSite wiki, @NonNull String username,
                                        @NonNull String password, @NonNull String retype,
                                        @NonNull String token, @Nullable String email,
                                        @Nullable String captchaId, @Nullable String captchaWord,
                                        @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), username, password, retype, token, email, captchaId, captchaWord, cb);
    }

    @SuppressWarnings("checkstyle:parameternumber") @VisibleForTesting
    Call<CreateAccountResponse> request(@NonNull Service service, @NonNull String username,
                                        @NonNull String password, @NonNull String retype,
                                        @NonNull String token, @Nullable String email,
                                        @Nullable String captchaId, @Nullable String captchaWord,
                                        @NonNull final Callback cb) {
        Call<CreateAccountResponse> call = service.postCreateAccount(username, password, retype, token,
                Service.WIKIPEDIA_URL, email, captchaId, captchaWord);
        call.enqueue(new retrofit2.Callback<CreateAccountResponse>() {
            @Override
            public void onResponse(@NonNull Call<CreateAccountResponse> call, @NonNull Response<CreateAccountResponse> response) {
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
            public void onFailure(@NonNull Call<CreateAccountResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }
}
