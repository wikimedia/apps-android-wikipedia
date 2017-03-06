package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class UnsuccessfulResponseInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Response rsp = chain.proceed(chain.request());
        if (rsp.isSuccessful()) {
            return rsp;
        }
        throw new HttpStatusException(rsp);
    }
}
