package org.wikipedia;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.concurrency.SaneAsyncTask;

import java.util.concurrent.Executor;

abstract public class ApiTask<T> extends SaneAsyncTask<T> {
    private final Api api;

    public ApiTask(Executor executor, Api api) {
        super(executor);
        this.api = api;
    }

    @Override
    public T performTask() throws Throwable {
        ApiResult result = makeRequest(buildRequest(api));
        return processResult(result);
    }

    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.get();
    }

    abstract public RequestBuilder buildRequest(Api api);
    abstract public T processResult(ApiResult result) throws Throwable;

}
