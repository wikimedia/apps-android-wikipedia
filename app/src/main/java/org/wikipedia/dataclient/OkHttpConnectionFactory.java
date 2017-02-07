package org.wikipedia.dataclient;

import android.support.annotation.NonNull;

import com.github.kevinsawicki.http.HttpRequest;

import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpConnectionFactory implements HttpRequest.ConnectionFactory {
    private static final long HTTP_CACHE_SIZE = 64 * 1024 * 1024;
    @NonNull private static final Cache HTTP_CACHE = new Cache(WikipediaApp.getInstance().getCacheDir(),
            HTTP_CACHE_SIZE);
    @NonNull private static final OkHttpClient CLIENT = createClient();

    @NonNull public static OkHttpClient getClient() {
        return CLIENT;
    }

    @Override
    public HttpURLConnection create(URL url) throws IOException {
        return new OkUrlFactory(getClient()).open(url); // TODO: update to newer API
    }

    @Override
    public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
        throw new UnsupportedOperationException(
                "Per-connection proxy is not supported. Use OkHttpClient's setProxy instead.");
    }

    @NonNull
    private static OkHttpClient createClient() {
        SharedPreferenceCookieManager cookieManager = WikipediaApp.getInstance().getCookieManager();
        // TODO: consider using okhttp3.CookieJar implementation instead of JavaNetCookieJar wrapper
        CookieJar cookieJar = new JavaNetCookieJar(cookieManager);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Prefs.getRetrofitLogLevel());

        return new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .cache(HTTP_CACHE)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new CommonHeaderInterceptor())
                .addInterceptor(new DefaultMaxStaleInterceptor())
                .addNetworkInterceptor(new CacheResponseInterceptor())
                .build();
    }

    @NonNull private static CacheControl cacheControlWithDefaultMaximumStale(@NonNull CacheControl cacheControl) {
        int maxStaleSeconds = cacheControl.maxStaleSeconds() < 0
                ? Integer.MAX_VALUE
                : cacheControl.maxStaleSeconds();
        return newCacheControlBuilder(cacheControl)
                .maxStale(maxStaleSeconds, TimeUnit.SECONDS)
                .build();
    }

    @NonNull private static CacheControl.Builder newCacheControlBuilder(@NonNull CacheControl cacheControl) {
        CacheControl.Builder builder = new CacheControl.Builder();
        if (cacheControl.noCache()) {
            builder.noCache();
        }
        if (cacheControl.noStore()) {
            builder.noStore();
        }
        if (cacheControl.maxAgeSeconds() >= 0) {
            builder.maxAge(cacheControl.maxAgeSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.maxStaleSeconds() >= 0) {
            builder.maxStale(cacheControl.maxStaleSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.minFreshSeconds() >= 0) {
            builder.minFresh(cacheControl.minFreshSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.onlyIfCached()) {
            builder.onlyIfCached();
        }
        if (cacheControl.noTransform()) {
            builder.noTransform();
        }
        return builder;
    }

    /** Sets a default max-stale cache-control argument on all requests that do not specify one */
    private static class DefaultMaxStaleInterceptor implements Interceptor {
        @Override public Response intercept(Chain chain) throws IOException {
            Request req = chain.request();

            CacheControl cacheControl = cacheControlWithDefaultMaximumStale(req.cacheControl());
            req = req.newBuilder().cacheControl(cacheControl).build();

            return chain.proceed(req);
        }
    }

    /** Allow response caching expressly strictly forbidden */
    private static class CacheResponseInterceptor implements Interceptor {
        @Override public Response intercept(Interceptor.Chain chain) throws IOException {
            Request req = chain.request();
            Response rsp = chain.proceed(req);
            // todo: remove restbase exception when endpoint doesn't respond with
            //       must-revalidate
            boolean restbase = req.url().toString().contains("/rest_v1/");
            if (!rsp.cacheControl().noStore()
                    && (!rsp.cacheControl().mustRevalidate() || restbase)) {
                CacheControl cacheControl = cacheControlWithDefaultMaximumStale(rsp.cacheControl());
                rsp = rsp.newBuilder().header("Cache-Control", cacheControl.toString()).build();
            }

            return rsp;
        }
    }

    // If adding a new header here, make sure to duplicate it in the MWAPI header builder
    // (WikipediaApp.buildCustomHeadersMap()).
    // TODO: remove above comment once buildCustomHeadersMap() is removed.
    private static class CommonHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            WikipediaApp app = WikipediaApp.getInstance();
            Request request = chain.request();
            request = request.newBuilder()
                    .header("User-Agent", app.getUserAgent())
                    .header(app.isEventLoggingEnabled() ? "X-WMF-UUID" : "DNT",
                            app.isEventLoggingEnabled() ? app.getAppInstallID() : "1")
                    .build();
            return chain.proceed(request);
        }
    }
}
