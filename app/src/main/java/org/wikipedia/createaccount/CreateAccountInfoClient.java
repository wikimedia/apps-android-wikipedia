package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class CreateAccountInfoClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull CreateAccountInfoResult result);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.getAuthManagerInfo();
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    String token = response.body().query().createAccountToken();
                    // noinspection ConstantConditions
                    String captchaId = response.body().query().captchaId();
                    cb.success(call, new CreateAccountInfoResult(token, captchaId));
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }
}
