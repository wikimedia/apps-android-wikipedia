package org.wikipedia.dataclient.okhttp;

import org.wikipedia.WikipediaApp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

// If adding a new header here, make sure to duplicate it in the MWAPI header builder
// (WikipediaApp.buildCustomHeadersMap()).
// TODO: remove above comment once buildCustomHeadersMap() is removed.
class CommonHeaderRequestInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
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