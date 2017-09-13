package org.wikipedia.dataclient.okhttp;

import org.wikipedia.WikipediaApp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.wikipedia.settings.Prefs.isEventLoggingEnabled;

class CommonHeaderRequestInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        WikipediaApp app = WikipediaApp.getInstance();
        Request request = chain.request().newBuilder()
                .header("User-Agent", app.getUserAgent())
                .header(isEventLoggingEnabled() ? "X-WMF-UUID" : "DNT",
                        isEventLoggingEnabled() ? app.getAppInstallID() : "1")
                .build();
        return chain.proceed(request);
    }
}
