package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.okhttp.util.HttpUrlUtil;
import org.wikipedia.settings.RbSwitch;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Response;

public class StatusResponseInterceptor implements Interceptor {
    @NonNull private final RbSwitch cb;

    public StatusResponseInterceptor(@NonNull RbSwitch cb) {
        this.cb = cb;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        HttpUrl url = chain.request().url();

        Response rsp;
        try {
            rsp = chain.proceed(chain.request());
        } catch (Throwable t) {
            failure(url, t);
            throw t;
        }

        success(url);

        return rsp;
    }

    private void success(@NonNull HttpUrl url) {
        if (HttpUrlUtil.isMobileView(url)) {
            cb.onMwSuccess();
        }
    }

    private void failure(@NonNull HttpUrl url, @Nullable Throwable t) {
        if (HttpUrlUtil.isRestBase(url)) {
            cb.onRbRequestFailed(t);
        }
    }
}
