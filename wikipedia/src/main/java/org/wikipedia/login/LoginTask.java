package org.wikipedia.login;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;
import org.wikipedia.concurrency.SaneAsyncTask;

import java.util.concurrent.Executor;

public class LoginTask extends SaneAsyncTask<String> {
    private final String username;
    private final String password;
    private final Api api;

    public LoginTask(Context context, Site site, String username, String password) {
        super(ExecutorService.getSingleton().getExecutor(LoginTask.class, 1));
        api = ((WikipediaApp)context.getApplicationContext()).getAPIForSite(site);
        this.username = username;
        this.password = password;
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



