package org.wikipedia.dataclient;

import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.settings.Prefs;

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
    private static LruCache<Long, RestService> REST_SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);

    public static Service get(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (SERVICE_CACHE.get(hashCode) != null) {
            return SERVICE_CACHE.get(hashCode);
        }

        Retrofit r = createRetrofit(wiki, TextUtils.isEmpty(Prefs.getMediaWikiBaseUrl()) ? wiki.url() + "/" : Prefs.getMediaWikiBaseUrl());

        Service s = r.create(Service.class);
        SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    public static RestService getRest(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (REST_SERVICE_CACHE.get(hashCode) != null) {
            return REST_SERVICE_CACHE.get(hashCode);
        }

        Retrofit r = createRetrofit(wiki, TextUtils.isEmpty(Prefs.getRestbaseUriFormat())
                        ? wiki.url() + "/" + RestService.REST_API_PREFIX
                        : String.format(Prefs.getRestbaseUriFormat(), "https", wiki.authority()));

        RestService s = r.create(RestService.class);
        REST_SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    private static Retrofit createRetrofit(@NonNull WikiSite wiki, @NonNull String baseUrl) {
        return new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient().newBuilder()
                        .addInterceptor(new LanguageVariantHeaderInterceptor(wiki)).build())
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
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
