package org.wikipedia.login;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

public class LoginTask extends SaneAsyncTask<String> {
    private final String username;
    private final String password;
    private final Api api;
    private final WikipediaApp app;

    public LoginTask(Context context, Site site, String username, String password) {
        super(SINGLE_THREAD);
        app = (WikipediaApp)context.getApplicationContext();
        api = app.getAPIForSite(site);
        this.username = username;
        this.password = password;
    }

    @Override
    public void onFinish(String result) {
        // Clear the edit tokens - clears out any anon tokens we might have had
        app.getEditTokenStorage().clearAllTokens();

        // Set userinfo
        app.getUserInfoStorage().setUser(new User(username, password));
    }

    @Override
    public String performTask() throws Throwable {
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

        JSONObject result = req.asObject();
        return result.optJSONObject("login").optString("result");
    }
}



