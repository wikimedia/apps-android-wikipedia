package org.wikipedia.editing;

import android.content.Context;
import android.util.Log;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;

import java.util.concurrent.Executor;

public class RefreshCaptchaTask extends ApiTask<CaptchaEditResult> {
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
    public CaptchaEditResult processResult(ApiResult result) throws Throwable {
        Log.d("Wikipedia", result.asObject().toString(4));
        return new CaptchaEditResult(
                result.asObject()
                        .optJSONObject("fancycaptchareload")
                        .optString("index")
        );
    }
}
