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



