package org.wikipedia.createaccount;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;
import org.wikipedia.editing.*;
import org.wikipedia.interlanguage.*;

public class CreateAccountTokenFetchTask extends ApiTask<CreateAccountTokenFetchTask.CreateAccountTokenResult> {
    public static class CreateAccountTokenResult{
        private final CaptchaResult captchaResult;
        private final String token;

        public CreateAccountTokenResult(CaptchaResult captchaResult, String token) {
            this.captchaResult = captchaResult;
            this.token = token;
        }

        public CaptchaResult getCaptchaResult() {
            return captchaResult;
        }

        public String getToken() {
            return token;
        }
    }


    private final String username;
    private final String password;
    public CreateAccountTokenFetchTask(Context context, String username, String password) {
        super(
                ExecutorService.getSingleton().getExecutor(LangLinksFetchTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(
                        ((WikipediaApp)context.getApplicationContext()).getPrimarySite()
                )
        );
        this.username = username;
        this.password = password;
    }
    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("createaccount")
                .param("name", username)
                .param("password", password);
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post();
    }

    @Override
    public CreateAccountTokenResult processResult(ApiResult result) throws Throwable {
        JSONObject ca = result.asObject().optJSONObject("createaccount");
        String token = ca.optString("token");
        CaptchaResult captchaResult = new CaptchaResult(ca.optJSONObject("captcha").optString("id"));
        return new CreateAccountTokenResult(captchaResult, token);
    }
}
