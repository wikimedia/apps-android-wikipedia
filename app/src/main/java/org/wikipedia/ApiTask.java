package org.wikipedia;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.concurrency.SaneAsyncTask;

import java.util.Map;

public abstract class ApiTask<T> extends SaneAsyncTask<T> {
    private static final boolean VERBOSE = WikipediaApp.getInstance().isDevRelease();
    private final Api api;

    @VisibleForTesting
    public ApiTask(Api api) {
        this.api = api;
    }

    @Override
    public T performTask() throws Throwable {
        RequestBuilder request = buildRequest(api);
        if (VERBOSE) {
            Log.v("ApiTask", buildUrl(api.getApiUrl().toString(), request.getParams()));
        }
        ApiResult result = makeRequest(request);
        return processResult(result);
    }

    /**
     * Called when an exception is thrown in the background process.
     * <p/>
     * Called on the UI Thread.
     *
     * Default implementation just throws it as a RuntimeException, so exceptions are never swallowed.
     * Unless specific exceptions are handled.
     *
     * @param caught The exception that was thrown.
     */
    @Override
    public void onCatch(Throwable caught) {
        throw new RuntimeException(caught);
    }

    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.get();
    }

    public abstract RequestBuilder buildRequest(Api api);
    public abstract T processResult(ApiResult result) throws Throwable;


    private String buildUrl(String url, Map<String, String> params) {
        Uri.Builder builder = new Uri.Builder().encodedPath(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.appendQueryParameter(param.getKey(), param.getValue());
        }
        return builder.build().toString();
    }
}
