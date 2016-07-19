package org.wikipedia.createaccount;

import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.log.L;

public class CreateAccountInfoTask extends ApiTask<CreateAccountInfoResult> {

    public CreateAccountInfoTask() {
        super(WikipediaApp.getInstance().getSiteApi());
    }

    @VisibleForTesting
    public CreateAccountInfoTask(Api api) {
        super(api);
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("meta", "authmanagerinfo|tokens")
                .param("amirequestsfor", "create")
                .param("type", "createaccount");
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public CreateAccountInfoResult processResult(ApiResult result) throws Throwable {
        String token = null;
        String captchaId = null;
        try {
            JSONObject query = result.asObject().getJSONObject("query");

            token = query.getJSONObject("tokens").getString("createaccounttoken");

            JSONObject authManagerInfo = query.getJSONObject("authmanagerinfo");
            JSONArray requests = authManagerInfo.getJSONArray("requests");
            for (int i = 0; i < requests.length(); i++) {
                JSONObject request = requests.getJSONObject(i);
                if (request.getString("id").equals("CaptchaAuthenticationRequest")) {
                    JSONObject fields = request.getJSONObject("fields");
                    captchaId = fields.getJSONObject("captchaId").getString("value");
                    break;
                }
            }
        } catch (JSONException e) {
            L.e("Error parsing createaccountinfo json", e);
        }
        return new CreateAccountInfoResult(token, captchaId);
    }
}
