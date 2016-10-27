package org.wikipedia.editing;


import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

class CaptchaClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<Captcha> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, cb);
    }

    @VisibleForTesting Call<Captcha> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<Captcha> call = service.refreshCaptcha();
        call.enqueue(new retrofit2.Callback<Captcha>() {
            @Override
            public void onResponse(Call<Captcha> call, Response<Captcha> response) {
                if (response.isSuccessful()) {
                    cb.success(call, new CaptchaResult(response.body().captchaId()));
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
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

    @VisibleForTesting interface Service {
        /* Get a fresh Captcha ID. */
        @GET("w/api.php?action=fancycaptchareload&format=json&formatversion=2")
        Call<Captcha> refreshCaptcha();
    }
}
