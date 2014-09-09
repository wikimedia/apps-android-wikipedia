package org.wikipedia.beta;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.concurrency.SaneAsyncTask;

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

    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.get();
    }

    public abstract RequestBuilder buildRequest(Api api);
    public abstract T processResult(ApiResult result) throws Throwable;

}
