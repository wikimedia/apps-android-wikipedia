package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.wikipedia.settings.Prefs;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheControlRequestInterceptor implements Interceptor {
    // When max-stale is set (as is the case for us in order to allow loading cached content),
    // OkHttp's CacheStrategy class forbids a conditional network request; hence, we'll need to
    // manually force a network request where needed, and fall back if it fails.
    @Override public Response intercept(Chain chain) throws IOException {
        if (!Prefs.preferOfflineContent() || chain.request().cacheControl().noCache()) {
            Request req = forceNetRequest(chain.request());
            Response rsp = null;
            try {
                rsp = chain.proceed(req);
            } catch (IOException ignore) { }

            if (rsp != null && rsp.isSuccessful()) {
                return rsp;
            }
        }

        try {
            return chain.proceed(chain.request());
        } catch (IOException e) {
            throw e;
        }
    }

    @NonNull private Request forceNetRequest(@NonNull Request req) {
        String cacheControl = CacheControlUtil.forceNetRequest(req.cacheControl().toString());
        return req.newBuilder().header("Cache-Control", cacheControl).build();
    }
}
