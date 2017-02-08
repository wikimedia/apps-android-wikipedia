package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class CacheIfErrorInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();
        Response rsp = chain.proceed(req);

        // when max-stale is set, CacheStrategy forbids a conditional network request in
        // CacheInterceptor
        if (rsp == null || stale(rsp)) {
            Request netReq = forceNetRequest(req);
            Response netRsp = null;
            try {
                netRsp = chain.proceed(netReq);
            } catch (IOException ignore) { }

            if (netRsp != null && netRsp.isSuccessful()) {
                return netRsp;
            }
        }

        return rsp;
    }

    @NonNull static Request forceNetRequest(@NonNull Request req) {
        String cacheControl = CacheControlUtil.forceNetRequest(req.cacheControl().toString());
        return req.newBuilder().header("Cache-Control", cacheControl).build();
    }

    private boolean stale(@NonNull Response response) {
        final String staleRspCode = "110";
        for (String header : response.headers("Warning")) {
            if (header.startsWith(staleRspCode)) {
                return true;
            }
        }
        return false;
    }
}