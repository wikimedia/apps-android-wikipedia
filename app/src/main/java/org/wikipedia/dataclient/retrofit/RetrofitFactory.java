package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {
    public static Retrofit newInstance(@NonNull WikiSite wiki) {
        return newInstance(wiki.url() + "/", wiki);
    }

    public static Retrofit newInstance(@NonNull String endpoint, @NonNull WikiSite wiki) {
        return new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient().newBuilder()
                        .addInterceptor(new LanguageVariantHeaderInterceptor(wiki)).build())
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
    }

    private RetrofitFactory() { }

    private static class LanguageVariantHeaderInterceptor implements Interceptor {
        @NonNull private final WikiSite wiki;

        LanguageVariantHeaderInterceptor(@NonNull WikiSite wiki) {
            this.wiki = wiki;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            request = request.newBuilder()
                    .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(wiki))
                    .build();
            return chain.proceed(request);
        }
    }
}
