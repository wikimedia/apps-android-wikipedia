package org.wikipedia.dataclient.okhttp;

import org.wikipedia.dataclient.okhttp.util.HttpUrlUtil;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Response;

class StripMustRevalidateResponseInterceptor implements Interceptor {
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Response rsp = chain.proceed(chain.request());
        HttpUrl url = rsp.request().url();

        if (HttpUrlUtil.isRestBase(url) || HttpUrlUtil.isMobileView(url)) {
            String cacheControl = CacheControlUtil.removeDirective(rsp.cacheControl().toString(),
                    "must-revalidate");
            rsp = rsp.newBuilder().header("Cache-Control", cacheControl).build();
        }

        return rsp;
    }
}
