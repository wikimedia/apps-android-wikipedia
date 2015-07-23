package org.wikipedia.page.fetch;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;

/**
 * Fetches something from the server synchronously; i.e. no AsyncTask.
 */
public interface Fetcher<T>  {
    RequestBuilder buildRequest(Api api);

    T processResult(ApiResult result) throws Throwable;
}
