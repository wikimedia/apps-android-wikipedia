package org.wikipedia.zero;

import android.content.Context;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;

public class WikipediaZeroTask extends ApiTask<ZeroMessage> {

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
    public ZeroMessage processResult(ApiResult result) throws Throwable {

        try {
            JSONObject results = result.asObject();
            String msg = results.getString("message");
            String fg = results.getString("foreground");
            String bg = results.getString("background");
            return new ZeroMessage(msg, fg, bg);
        } catch (Exception e) {
            return null;
        }
    }


}