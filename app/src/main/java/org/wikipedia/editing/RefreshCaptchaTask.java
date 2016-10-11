package org.wikipedia.editing;

import android.content.Context;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.dataclient.ApiTask;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.WikipediaApp;

public class RefreshCaptchaTask extends ApiTask<CaptchaResult> {
    public RefreshCaptchaTask(Context context, WikiSite wiki) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(wiki));
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("fancycaptchareload");
    }

    @Override
    public CaptchaResult processResult(ApiResult result) throws Throwable {
        return new CaptchaResult(
                result.asObject()
                        .optJSONObject("fancycaptchareload")
                        .optString("index")
        );
    }
}
