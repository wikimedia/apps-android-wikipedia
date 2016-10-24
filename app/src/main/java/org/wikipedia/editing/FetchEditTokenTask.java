package org.wikipedia.editing;

import android.content.Context;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ApiTask;
import org.wikipedia.dataclient.WikiSite;

public class FetchEditTokenTask extends ApiTask<String> {
    public FetchEditTokenTask(Context context, WikiSite wiki) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(wiki));
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("tokens")
                .param("type", "edit");
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        return result.asObject().optJSONObject("tokens").optString("edittoken");
    }
}
