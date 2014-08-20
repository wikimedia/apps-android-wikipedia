package org.wikipedia.beta.zero;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.ApiTask;
import org.wikipedia.beta.WikipediaApp;

public class WikipediaZeroTask extends ApiTask<String> {

    private Context ctx;

    public WikipediaZeroTask(Api api, Context context) {
        super(SINGLE_THREAD, api);
        this.ctx = context;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("zeroconfig")
                .param("type", "message")
                .param("agent", ((WikipediaApp)ctx.getApplicationContext()).getUserAgent());
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        try {
            JSONObject results = result.asObject();
            String message = results.getString("message");
            return message;
        } catch (Exception e) {
            return null;
        }
    }
}