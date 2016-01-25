package org.wikipedia.createaccount;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.CaptchaResult;

public abstract class CreateAccountTask extends ApiTask<CreateAccountResult> {
    private final String username;
    private final String password;
    private final String email;

    private String token;

    public CreateAccountTask(Context context, String username, String password, String email) {
        super(((WikipediaApp)context.getApplicationContext()).getSiteApi());

        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder = api.action("createaccount")
                .param("name", username)
                .param("password", password);
        if (email != null) {
            builder.param("email", email);
        }
        if (token != null) {
            builder.param("token", token);
        }
        return builder;
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public CreateAccountResult processResult(ApiResult result) throws Throwable {
        JSONObject ca = result.asObject().optJSONObject("createaccount");
        String apiResult = ca.optString("result");
        switch (apiResult) {
            case "NeedToken":
                // We need to just repeat the request.
                // Set token and restart the request
                token = ca.optString("token");
                return performTask();
            case "NeedCaptcha":
                return new CreateAccountCaptchaResult(new CaptchaResult(ca.optJSONObject("captcha").optString("id")));
            case "Success":
                return new CreateAccountSuccessResult(ca.optString("username"));
            default:
                return new CreateAccountResult(apiResult);
        }
    }
}
