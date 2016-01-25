package org.wikipedia.login;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

public class LoginTask extends SaneAsyncTask<LoginResult> {
    private final String username;
    private final String password;
    private final Api api;
    private final WikipediaApp app;

    public LoginTask(Context context, Site site, String username, String password) {
        app = (WikipediaApp)context.getApplicationContext();
        api = app.getAPIForSite(site);
        this.username = username;
        this.password = password;
    }

    @Override
    public void onFinish(LoginResult result) {
        if (result.getCode().equals("Success")) {
            // Clear the edit tokens - clears out any anon tokens we might have had
            app.getEditTokenStorage().clearAllTokens();

            // Set userinfo
            app.getUserInfoStorage().setUser(result.getUser());
        }
    }

    @Override
    public LoginResult performTask() throws Throwable {
        // HACK: T124384
        app.getEditTokenStorage().clearAllTokens();

        ApiResult preReq = api.action("login")
                .param("lgname", username)
                .param("lgpassword", password)
                .post();
        JSONObject preReqResult = preReq.asObject();
        String token = preReqResult.optJSONObject("login").optString("token");

        ApiResult req = api.action("login")
                .param("lgname", username)
                .param("lgpassword", password)
                .param("lgtoken", token)
                .post();

        JSONObject result = req.asObject().optJSONObject("login");

        User user = null;
        if (result.optString("result").equals("Success")) {
            user = new User(result.optString("lgusername"), password, result.optInt("lguserid"));
        }
        return new LoginResult(result.optString("result"), user);
    }
}
