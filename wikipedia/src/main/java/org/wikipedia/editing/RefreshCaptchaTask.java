package org.wikipedia.editing;

import android.content.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class RefreshCaptchaTask extends ApiTask<CaptchaResult> {
    public RefreshCaptchaTask(Context context, Site site) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(site)
        );
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
