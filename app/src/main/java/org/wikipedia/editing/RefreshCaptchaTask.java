package org.wikipedia.editing;

import android.content.Context;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class RefreshCaptchaTask extends ApiTask<CaptchaResult> {
    public RefreshCaptchaTask(Context context, Site site) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(site));
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
