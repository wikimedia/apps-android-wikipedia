package org.wikipedia.createaccount.authmanager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.log.L;

import javax.security.auth.login.LoginException;

public abstract class AMCreateAccountTask extends ApiTask<AMCreateAccountResult> {
    @NonNull private final String username;
    @NonNull private final String password;
    @NonNull private final String repeatPassword;
    @Nullable private final String email;

    public AMCreateAccountTask(@NonNull String username, @NonNull String password,
                               @NonNull String repeatPassword, @Nullable String email) {
        super(WikipediaApp.getInstance().getSiteApi());
        this.username = username;
        this.password = password;
        this.repeatPassword = repeatPassword;
        this.email = email;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        JSONObject preReqResult;
        String token = "";
        try {
            ApiResult preReq = api.action("query")
                    .param("meta", "tokens")
                    .param("type", "createaccount")
                    .post();
            preReqResult = preReq.asObject();
            token = preReqResult.optJSONObject("query").optJSONObject("tokens")
                    .optString("createaccounttoken");
        } catch (ApiException e) {
            L.e("Failed to fetch createaccount token");
        }

        RequestBuilder builder = api.action("createaccount")
                .param("username", username)
                .param("password", password)
                .param("retype", repeatPassword)
                .param("createtoken", token)
                .param("createreturnurl", Constants.WIKIPEDIA_URL);
        if (email != null) {
            builder.param("email", email);
        }
        return builder;
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public AMCreateAccountResult processResult(ApiResult result) throws Throwable {
        JSONObject createaccount = result.asObject().optJSONObject("createaccount");
        String status = createaccount.optString("status");
        String message = null;
        switch(status) {
            case "PASS":
                return new AMCreateAccountSuccessResult(createaccount.optString("username"));
            case "UI":
            case "RESTART":
            case "FAIL":
                message = createaccount.optString("message");
                if (message.contains("CAPTCHA")) {
                    return new AMCreateAccountCaptchaResult();
                }
                return new AMCreateAccountResult(status, message);
            default:
                throw new LoginException("Login failed: received unknown login status");
        }
    }
}
