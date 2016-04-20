package org.wikipedia.login.authmanager;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;

public class AMLoginInfoTask extends ApiTask<AMLoginInfoResult> {

    public AMLoginInfoTask() {
        super(WikipediaApp.getInstance().getSiteApi());
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                  .param("meta", "authmanagerinfo")
                  .param("amirequestsfor", "login");

    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public AMLoginInfoResult processResult(ApiResult result) throws Throwable {
        boolean enabled = result.asObject().optJSONObject("query") != null;
        // TODO: do more parsing here if/when we need the content
        return new AMLoginInfoResult(enabled);
    }
}
