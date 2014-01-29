package org.wikipedia;

import android.content.Context;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;

public class OkHttpConnectionFactory implements HttpRequest.ConnectionFactory {
    private static final long HTTP_CACHE_SIZE = 16 * 1024 * 1024;

    private final OkHttpClient client;

    public OkHttpConnectionFactory(Context context) {
        client = new OkHttpClient();
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(cm);

        try {
            client.setResponseCache(new HttpResponseCache(context.getCacheDir(), HTTP_CACHE_SIZE));
        } catch (IOException e) {
            // Shouldn't happen...
            throw new RuntimeException(e);
        }
    }

    public HttpURLConnection create(URL url) throws IOException {
        return client.open(url);
    }

    @Override
    public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
        throw new UnsupportedOperationException(
                "Per-connection proxy is not supported. Use OkHttpClient's setProxy instead.");
    }
}
