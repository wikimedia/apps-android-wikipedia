package org.wikipedia;

import org.mediawiki.api.json.*;
import org.wikipedia.concurrency.*;

public abstract class ApiTask<T> extends SaneAsyncTask<T> {
    private final Api api;

    public ApiTask(int threadCount, Api api) {
        super(threadCount);
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
