package org.wikipedia.random;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;

public class RandomArticleIdTask extends ApiTask<PageTitle> {
    private Site site;

    public RandomArticleIdTask(Api api, Site site) {
        super(api);
        this.site = site;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("list", "random")
                .param("rnnamespace", "0")
                .param("rnlimit", "1") // maybe we grab 10 in the future and persist it somewhere
                .param("continue", ""); // to avoid warning about new continuation syntax
    }

    @Override
    public PageTitle processResult(ApiResult result) throws Throwable {
        JSONArray results = result.asObject().optJSONObject("query").optJSONArray("random");
        JSONObject random = (JSONObject)results.get(0);
        return new PageTitle(null, random.getString("title"), site);
    }
}