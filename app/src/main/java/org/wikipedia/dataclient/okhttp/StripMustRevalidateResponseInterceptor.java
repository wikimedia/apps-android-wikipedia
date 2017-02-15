package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

class StripMustRevalidateResponseInterceptor implements Interceptor {
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Response rsp = chain.proceed(chain.request());
        String url = rsp.request().url().toString();

        boolean restBase = url.contains("/rest_v1/");
        boolean mobileView = url.contains("action=mobileview");
        if (restBase || mobileView) {
            String cacheControl = CacheControlUtil.removeDirective(rsp.cacheControl().toString(),
                    "must-revalidate");
            rsp = rsp.newBuilder().header("Cache-Control", cacheControl).build();
        }

        return rsp;
    }
}
