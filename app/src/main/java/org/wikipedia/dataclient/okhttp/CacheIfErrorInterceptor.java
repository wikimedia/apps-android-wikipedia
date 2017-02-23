package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class CacheIfErrorInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Response rsp = null;
        try {
            rsp = chain.proceed(chain.request().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build());
        } catch (IOException ignore) { }

        // when max-stale is set, CacheStrategy forbids a conditional network request in
        // CacheInterceptor
        if (!chain.request().cacheControl().onlyIfCached()) {
            Request netReq = forceNetRequest(chain.request());
            Response netRsp = null;
            try {
                netRsp = chain.proceed(netReq);
            } catch (IOException ignore) {
                if (rsp == null) {
                    throw ignore;
                }
            }

            if (netRsp != null && (netRsp.isSuccessful() || rsp == null || !rsp.isSuccessful())) {
                return netRsp;
            }
        }

        return rsp;
    }

    @NonNull private Request forceNetRequest(@NonNull Request req) {
        String cacheControl = CacheControlUtil.forceNetRequest(req.cacheControl().toString());
        return req.newBuilder().header("Cache-Control", cacheControl).build();
    }
}
