package org.wikipedia.login;

import android.content.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class LogoutTask extends ApiTask<Boolean> {
    private final WikipediaApp app;

    public LogoutTask(Context context, Site site) {
        super(
                1,
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
