package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.wikipedia.dataclient.okhttp.CacheControlUtil.cacheControlWithDefaultMaximumStale;

/** Allow response caching expressly strictly forbidden */
class CacheResponseInterceptor implements Interceptor {
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request req = chain.request();
        Response rsp = chain.proceed(req);
        // todo: remove restbase exception when endpoint doesn't respond with
        //       must-revalidate
        boolean restbase = req.url().toString().contains("/rest_v1/");
        if (!rsp.cacheControl().noStore()
                && (!rsp.cacheControl().mustRevalidate() || restbase)) {
            CacheControl cacheControl = cacheControlWithDefaultMaximumStale(rsp.cacheControl());
            rsp = rsp.newBuilder().header("Cache-Control", cacheControl.toString()).build();
        }

        return rsp;
    }
}