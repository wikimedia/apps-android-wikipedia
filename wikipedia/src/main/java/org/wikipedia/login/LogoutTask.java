package org.wikipedia.login;

import android.content.Context;
import android.util.Log;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;

import java.util.concurrent.Executor;

public class LogoutTask extends ApiTask<Boolean> {
    private final WikipediaApp app;

    public LogoutTask(Context context, Site site) {
        super(
                ExecutorService.getSingleton().getExecutor(LogoutTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(site)
        );
        app = (WikipediaApp)context.getApplicationContext();

    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("logout");
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post();
    }

    @Override
    public Boolean processResult(ApiResult result) throws Throwable {
        // Doesn't actually trigger the request until I call this
        // Stupid API design on Yuvi's part
        result.asArray();
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        app.getUserInfoStorage().clearUser();
        return true;
    }
}
