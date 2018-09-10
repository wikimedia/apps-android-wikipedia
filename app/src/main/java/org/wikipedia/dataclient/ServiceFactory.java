package org.wikipedia.dataclient;

import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ServiceFactory {
    private static final int SERVICE_CACHE_SIZE = 8;
    private static LruCache<Long, Service> SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);

    public static Service get(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (SERVICE_CACHE.get(hashCode) != null) {
            return SERVICE_CACHE.get(hashCode);
        }

        Retrofit r = new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient().newBuilder()
                        .addInterceptor(new LanguageVariantHeaderInterceptor(wiki)).build())
                .baseUrl(wiki.url() + "/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();

        Service s = r.create(Service.class);
        SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    private ServiceFactory() { }

    private static class LanguageVariantHeaderInterceptor implements Interceptor {
        @NonNull private final WikiSite wiki;

        LanguageVariantHeaderInterceptor(@NonNull WikiSite wiki) {
            this.wiki = wiki;
        }

        @Override
        public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            request = request.newBuilder()
                    .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(wiki))
                    .build();
            return chain.proceed(request);
        }
    }
}
