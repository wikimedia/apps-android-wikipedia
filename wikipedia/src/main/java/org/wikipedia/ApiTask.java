package org.wikipedia;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.util.NetworkUtils;

import javax.net.ssl.SSLException;

public abstract class ApiTask<T> extends SaneAsyncTask<T> {
    private final Api api;

    public ApiTask(int concurrency, Api api) {
        super(concurrency);
        this.api = api;
    }

    @Override
    public T performTask() throws Throwable {
        ApiResult result = makeRequest(buildRequest(api));
        return processResult(result);
    }

    /**
     * Called when an exception is thrown in the background process.  Checks whether the exception
     * is an SSLException and, if so, prompts user to try again if the SSLException is the first.
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
        if (Utils.throwableContainsSpecificType(caught, SSLException.class)
                && WikipediaApp.getInstance().incSslFailCount() < 2) {
            WikipediaApp.getInstance().setSslFallback(true);
            if (!isCancelled()) {
                NetworkUtils.toastNetworkFail();
            }
            cancel();
            return;
        }
        throw new RuntimeException(caught);
    }

    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.get();
    }

    public abstract RequestBuilder buildRequest(Api api);
    public abstract T processResult(ApiResult result) throws Throwable;

}
