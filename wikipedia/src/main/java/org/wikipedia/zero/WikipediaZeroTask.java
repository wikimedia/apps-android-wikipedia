package org.wikipedia.zero;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.concurrency.ExecutorService;

public class WikipediaZeroTask extends ApiTask<Boolean> {

    public WikipediaZeroTask(Api api) {
        super(ExecutorService.getSingleton().getExecutor(WikipediaZeroTask.class, 1), api);
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("zeroconfig").param("type", "config");
    }

    @Override
    public Boolean processResult(ApiResult result) throws Throwable {
        JSONObject results = result.asObject();
        boolean enabled = results.getBoolean("enabled");
        if (enabled) {
            enabled = results.getBoolean("enableHttps");
        }
        return enabled;
    }
}