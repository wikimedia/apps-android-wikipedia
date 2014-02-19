package org.wikipedia.editing;

import android.content.*;
import android.util.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

public class RefreshCaptchaTask extends ApiTask<CaptchaResult> {
    public RefreshCaptchaTask(Context context, PageTitle title) {
        super(
                ExecutorService.getSingleton().getExecutor(DoEditTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("fancycaptchareload");
    }

    @Override
    public CaptchaResult processResult(ApiResult result) throws Throwable {
        Log.d("Wikipedia", result.asObject().toString(4));
        return new CaptchaResult(
                result.asObject()
                        .optJSONObject("fancycaptchareload")
                        .optString("index")
        );
    }
}
