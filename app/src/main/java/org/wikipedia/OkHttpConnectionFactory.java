package org.wikipedia;

import android.support.annotation.NonNull;

import com.github.kevinsawicki.http.HttpRequest;

import org.wikipedia.settings.Prefs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import okhttp3.Cache;
import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpConnectionFactory implements HttpRequest.ConnectionFactory {
    private static final long HTTP_CACHE_SIZE = 64 * 1024 * 1024;
    private static final Cache HTTP_CACHE = new Cache(WikipediaApp.getInstance().getCacheDir(), HTTP_CACHE_SIZE);
    @NonNull private static final OkHttpClient CLIENT = createClient();

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
    public static OkHttpClient getClient() {
        return CLIENT;
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
                .build();
    }
}
