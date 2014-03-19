package org.wikipedia.createaccount;

import android.content.*;
import android.util.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.editing.*;

public class CreateAccountTask extends ApiTask<CreateAccountResult> {


    private final String username;
    private final String password;
    private final String email;
    private final String token;
    public CreateAccountTask(Context context, String username, String password, String email, String token) {
        super(
                1,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(
                        ((WikipediaApp)context.getApplicationContext()).getPrimarySite()
                )
        );
        this.username = username;
        this.password = password;
        this.email = email;
        this.token = token;
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
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post();
    }

    @Override
    public CreateAccountResult processResult(ApiResult response) throws Throwable {
        Log.d("Wikipedia", response.asObject().toString(4));
        if (response.asObject().has("error")) {
            return new CreateAccountResult(response.asObject().optJSONObject("error").optString("code"));
        }
        JSONObject ca = response.asObject().optJSONObject("createaccount");
        String result = ca.optString("result");
        if (result.toLowerCase().equals("needtoken")) {
            String token = ca.optString("token");
            CaptchaResult captchaResult = new CaptchaResult(ca.optJSONObject("captcha").optString("id"));
            return new CreateAccountTokenResult(captchaResult, token);
        } else {
            return new CreateAccountResult(result);
        }
    }
}
