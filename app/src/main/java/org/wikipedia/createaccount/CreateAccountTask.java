package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;

import javax.security.auth.login.LoginException;

public abstract class CreateAccountTask extends ApiTask<CreateAccountResult> {
    @NonNull private final String username;
    @NonNull private final String password;
    @NonNull private final String repeatPassword;
    @NonNull private final String token;
    @Nullable private final String email;

    @VisibleForTesting
    public CreateAccountTask(@NonNull String username, @NonNull String password,
                             @NonNull String repeatPassword, @NonNull String token,
                             @Nullable String email) {
        super(WikipediaApp.getInstance().getSiteApi());
        this.username = username;
        this.password = password;
        this.repeatPassword = repeatPassword;
        this.token = token;
        this.email = email;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
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
    public CreateAccountResult processResult(ApiResult result) throws Throwable {
        JSONObject createaccount = result.asObject().optJSONObject("createaccount");
        String status = createaccount.optString("status");
        switch(status) {
            case "PASS":
                return new CreateAccountSuccessResult(createaccount.optString("username"));
            case "UI":
            case "RESTART":
            case "FAIL":
                return new CreateAccountResult(status, createaccount.optString("message"));
            default:
                throw new LoginException("Account creation failed");
        }
    }
}
