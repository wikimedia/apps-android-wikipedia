package org.wikipedia.login.authmanager;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.login.User;
import org.wikipedia.util.log.L;

public abstract class AMLoginTask extends ApiTask<AMLoginResult> {
    private final String username;
    private final String password;

    public AMLoginTask(String username, String password) {
        super(WikipediaApp.getInstance().getSiteApi());
        this.username = username;
        this.password = password;
    }

    @Override public RequestBuilder buildRequest(Api api) {
        // HACK: T124384
        WikipediaApp.getInstance().getEditTokenStorage().clearAllTokens();

        JSONObject preReqResult;
        String token = "";
        try {
            ApiResult preReq = api.action("query")
                    .param("meta", "tokens")
                    .param("type", "login")
                    .post();
            preReqResult = preReq.asObject();
            token = preReqResult.optJSONObject("query")
                    .optJSONObject("tokens")
                    .optString("logintoken");
        } catch (ApiException e) {
            L.e("Failed to fetch login token");
        }

        return api.action("clientlogin")
                .param("username", username)
                .param("password", password)
                .param("logintoken", token)
                .param("loginreturnurl", Constants.WIKIPEDIA_URL);
    }

    @Override
    public void onFinish(AMLoginResult result) {
        if (result.pass()) {
            WikipediaApp.getInstance().getUserInfoStorage().setUser(result.getUser());
        }
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public AMLoginResult processResult(ApiResult result) throws Throwable {
        JSONObject clientlogin = result.asObject().optJSONObject("clientlogin");
        User user = null;
        String message = null;
        if ("PASS".equals(clientlogin.optString("status"))) {
            user = new User(clientlogin.optString("username"), password, 0);
        } else if ("FAIL".equals(clientlogin.optString("status"))) {
            message = clientlogin.optString("message");
        }
        return new AMLoginResult(clientlogin.optString("status"), user, message);
    }
}
