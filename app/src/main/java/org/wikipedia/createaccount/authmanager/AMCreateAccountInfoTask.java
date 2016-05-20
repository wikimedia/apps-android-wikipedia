package org.wikipedia.createaccount.authmanager;

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

public class AMCreateAccountInfoTask extends ApiTask<AMCreateAccountInfoResult> {

    public AMCreateAccountInfoTask() {
        super(WikipediaApp.getInstance().getSiteApi());
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("meta", "authmanagerinfo")
                .param("amirequestsfor", "create");

    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public AMCreateAccountInfoResult processResult(ApiResult result) throws Throwable {
        boolean enabled = result.asObject().optJSONObject("query") != null;
        if (!enabled) {
            return new AMCreateAccountInfoResult(false, null);
        }

        String captchaId = null;
        try {
            JSONObject query = result.asObject().getJSONObject("query");
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
        return new AMCreateAccountInfoResult(true, captchaId);
    }
}
