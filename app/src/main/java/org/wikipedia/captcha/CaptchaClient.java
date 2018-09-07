package org.wikipedia.captcha;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class CaptchaClient {
    public Call<Captcha> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), cb);
    }

    @VisibleForTesting Call<Captcha> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<Captcha> call = service.getNewCaptcha();
        call.enqueue(new retrofit2.Callback<Captcha>() {
            @Override
            public void onResponse(Call<Captcha> call, Response<Captcha> response) {
                if (response.body().success()) {
                    cb.success(call, new CaptchaResult(response.body().captchaId()));
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<Captcha> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<Captcha> call, @NonNull CaptchaResult result);
        void failure(@NonNull Call<Captcha> call, @NonNull Throwable caught);
    }
}
