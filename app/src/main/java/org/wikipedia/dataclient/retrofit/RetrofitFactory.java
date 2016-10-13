package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.GsonUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {
    public static Retrofit newInstance(@NonNull Site site) {
        return newInstance(site.url() + "/", site);
    }

    public static Retrofit newInstance(@NonNull String endpoint, @NonNull Site site) {
        return new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient().newBuilder()
                        .addInterceptor(new LanguageVariantHeaderInterceptor(site)).build())
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
    }

    private RetrofitFactory() { }

    private static class LanguageVariantHeaderInterceptor implements Interceptor {
        private final Site site;

        LanguageVariantHeaderInterceptor(@NonNull Site site) {
            this.site = site;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            request = request.newBuilder()
                    .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(site))
                    .build();
            return chain.proceed(request);
        }
    }
}
