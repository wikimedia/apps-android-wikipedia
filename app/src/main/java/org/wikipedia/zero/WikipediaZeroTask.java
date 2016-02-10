package org.wikipedia.zero;

import android.graphics.Color;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;

public class WikipediaZeroTask extends ApiTask<ZeroConfig> {
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
    public ZeroConfig processResult(ApiResult result) throws Throwable {
        try {
            JSONObject results = result.asObject();
            String message = results.getString("message");
            String foreground = results.getString("foreground");
            String background = results.getString("background");
            String exitTitle = results.optString("exitTitle");
            String exitWarning = results.optString("exitWarning");
            String partnerInfoText = results.optString("partnerInfoText");
            String partnerInfoUrl = results.optString("partnerInfoUrl");
            String bannerUrl = results.optString("bannerUrl");

            return new ZeroConfig.Builder(message, Color.parseColor(foreground), Color.parseColor(background))
                    .exitTitle(exitTitle)
                    .exitWarning(exitWarning)
                    .partnerInfoText(partnerInfoText)
                    .partnerInfoUrl(partnerInfoUrl)
                    .bannerUrl(bannerUrl)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
