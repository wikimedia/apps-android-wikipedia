package org.wikipedia.zero;

import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;

public class WikipediaZeroTask extends ApiTask<ZeroMessage> {
    private String userAgent;

    // TODO: should user agent be exposed from Api?
    @VisibleForTesting
    public WikipediaZeroTask(Api api, String userAgent) {
        super(api);
        init(userAgent);
    }

    private void init(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("zeroconfig")
                .param("type", "message")
                .param("agent", userAgent);
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
