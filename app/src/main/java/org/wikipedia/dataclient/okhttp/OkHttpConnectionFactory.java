package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.github.kevinsawicki.http.HttpRequest;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.RbSwitch;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import okhttp3.Cache;
import okhttp3.CacheDelegate;
import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
import okhttp3.internal.cache.CacheDelegateInterceptor;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpConnectionFactory implements HttpRequest.ConnectionFactory {
    private static final String CACHE_DIR_NAME = "okhttp-cache";
    private static final long NET_CACHE_SIZE = 64 * 1024 * 1024;
    @VisibleForTesting @NonNull public static final Cache NET_CACHE = new Cache(new File(WikipediaApp.getInstance().getCacheDir(),
            CACHE_DIR_NAME), NET_CACHE_SIZE);
    private static final long SAVED_PAGE_CACHE_SIZE = NET_CACHE_SIZE * 1024;
    @NonNull public static final Cache SAVE_CACHE = new Cache(new File(WikipediaApp.getInstance().getFilesDir(),
            CACHE_DIR_NAME), SAVED_PAGE_CACHE_SIZE);

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

        return new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .cache(NET_CACHE)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(Prefs.getRetrofitLogLevel()))
                .addInterceptor(new UnsuccessfulResponseInterceptor())
                .addInterceptor(new StatusResponseInterceptor(RbSwitch.INSTANCE))
                .addNetworkInterceptor(new StripMustRevalidateResponseInterceptor())
                .addInterceptor(new CommonHeaderRequestInterceptor())
                .addInterceptor(new DefaultMaxStaleRequestInterceptor())
                .addInterceptor(new CacheIfErrorInterceptor())
                .addInterceptor(new CacheDelegateInterceptor(CacheDelegate.internalCache(SAVE_CACHE), CacheDelegate.internalCache(NET_CACHE)))
                .addInterceptor(new WikipediaZeroResponseInterceptor(WikipediaApp.getInstance().getWikipediaZeroHandler()))
                .build();
    }
}
