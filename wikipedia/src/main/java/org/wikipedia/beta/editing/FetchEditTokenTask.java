package org.wikipedia.beta.editing;

import android.content.Context;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.ApiTask;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;

public class FetchEditTokenTask extends ApiTask<String> {
    public FetchEditTokenTask(Context context, Site site) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(site)
        );
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
