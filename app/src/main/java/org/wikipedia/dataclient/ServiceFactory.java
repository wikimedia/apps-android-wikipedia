package org.wikipedia.dataclient;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.eventplatform.DestinationEventService;
import org.wikipedia.analytics.eventplatform.EventService;
import org.wikipedia.analytics.eventplatform.StreamConfig;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.settings.Prefs;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.wikipedia.settings.Prefs.getEventPlatformIntakeUriOverride;

public final class ServiceFactory {
    private static final int SERVICE_CACHE_SIZE = 8;
    private static LruCache<Long, Service> SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);
    private static LruCache<Long, RestService> REST_SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);
    private static LruCache<Long, CoreRestService> CORE_REST_SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);
    private static LruCache<String, EventService> ANALYTICS_REST_SERVICE_CACHE = new LruCache<>(SERVICE_CACHE_SIZE);

    public static Service get(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (SERVICE_CACHE.get(hashCode) != null) {
            return SERVICE_CACHE.get(hashCode);
        }
        Retrofit r = createRetrofit(wiki, getBasePath(wiki));
        Service s = r.create(Service.class);
        SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    public static RestService getRest(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (REST_SERVICE_CACHE.get(hashCode) != null) {
            return REST_SERVICE_CACHE.get(hashCode);
        }
        Retrofit r = createRetrofit(wiki, getRestBasePath(wiki));
        RestService s = r.create(RestService.class);
        REST_SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    public static CoreRestService getCoreRest(@NonNull WikiSite wiki) {
        long hashCode = wiki.hashCode();
        if (CORE_REST_SERVICE_CACHE.get(hashCode) != null) {
            return CORE_REST_SERVICE_CACHE.get(hashCode);
        }
        Retrofit r = createRetrofit(wiki, wiki.url() + "/" + CoreRestService.CORE_REST_API_PREFIX);
        CoreRestService s = r.create(CoreRestService.class);
        CORE_REST_SERVICE_CACHE.put(hashCode, s);
        return s;
    }

    public static EventService getAnalyticsRest(@NonNull StreamConfig streamConfig) {
        DestinationEventService destinationEventService = streamConfig.getDestinationEventService();
        if (destinationEventService == null) {
            destinationEventService = DestinationEventService.ANALYTICS;
        }
        if (ANALYTICS_REST_SERVICE_CACHE.get(destinationEventService.getId()) != null) {
            return ANALYTICS_REST_SERVICE_CACHE.get(destinationEventService.getId());
        }

        String intakeBaseUriOverride = getEventPlatformIntakeUriOverride();

        Retrofit r = createRetrofit(null, StringUtils.defaultString(intakeBaseUriOverride, destinationEventService.getBaseUri()));
        EventService s = r.create(EventService.class);
        ANALYTICS_REST_SERVICE_CACHE.put(destinationEventService.getId(), s);
        return s;
    }

    public static <T> T get(@NonNull WikiSite wiki, @Nullable String baseUrl, Class<T> service) {
        Retrofit r = createRetrofit(wiki, TextUtils.isEmpty(baseUrl) ? wiki.url() + "/" : baseUrl);
        return r.create(service);
    }

    private static String getBasePath(@NonNull WikiSite wiki) {
        return TextUtils.isEmpty(Prefs.getMediaWikiBaseUrl()) ? wiki.url() + "/" : Prefs.getMediaWikiBaseUrl();
    }

    public static String getRestBasePath(@NonNull WikiSite wiki) {
        String path = TextUtils.isEmpty(Prefs.getRestbaseUriFormat())
                ? wiki.url() + "/" + RestService.REST_API_PREFIX
                : String.format(Prefs.getRestbaseUriFormat(), "https", wiki.authority());
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path;
    }

    private static Retrofit createRetrofit(@Nullable WikiSite wiki, @NonNull String baseUrl) {
        OkHttpClient.Builder okHttpClientBuilder = OkHttpConnectionFactory.getClient().newBuilder();
        if (wiki != null) {
            okHttpClientBuilder.addInterceptor(new LanguageVariantHeaderInterceptor(wiki));
        }
        return new Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(baseUrl)
                .client(OkHttpConnectionFactory.getClient().newBuilder().addInterceptor(new LanguageVariantHeaderInterceptor(wiki)).build())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
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

            // TODO: remove when the https://phabricator.wikimedia.org/T271145 is resolved.
            if (!request.url().encodedPath().contains("/page/related")) {
                request = request.newBuilder()
                        .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(wiki))
                        .build();
            }

            return chain.proceed(request);
        }
    }
}
