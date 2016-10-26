package org.wikipedia.editing;


import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

class CaptchaClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);
    @NonNull private final Retrofit retrofit = RbPageEndpointsCache.INSTANCE.getRetrofit();

    public Call<Captcha> request(@NonNull final WikiSite wiki, @NonNull final Callback cb) {
        Call<Captcha> call = cachedService.service(wiki).refreshCaptcha();
        call.enqueue(new retrofit2.Callback<Captcha>() {
            @Override
            public void onResponse(Call<Captcha> call, Response<Captcha> response) {
                if (response.isSuccessful()) {
                    cb.success(call, new CaptchaResult(response.body().captchaId()));
                } else {
                    cb.failure(call, RetrofitException.httpError(response, retrofit));
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

    private interface Service {
        /* Get a fresh Captcha ID. */
        @GET("w/api.php?action=fancycaptchareload&format=json&formatversion=2")
        Call<Captcha> refreshCaptcha();
    }
}
