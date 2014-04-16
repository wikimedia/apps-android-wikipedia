package org.wikipedia.random;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class RandomArticleIdTask extends ApiTask<PageTitle> {

    private Context ctx;
    private Site site;

    public RandomArticleIdTask(Api api, Site site, Context context) {
        super(SINGLE_THREAD, api);
        this.site = site;
        this.ctx = context;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("list", "random")
                .param("rnnamespace", "0")
                .param("rnlimit", "1"); // maybe we grab 10 in the future and persist it somewhere
    }

    @Override
    public PageTitle processResult(ApiResult result) throws Throwable {
        try {
            JSONArray results = result.asObject().optJSONObject("query").optJSONArray("random");
            JSONObject random = (JSONObject)results.get(0);

            if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
                Utils.processHeadersForZero((WikipediaApp)ctx, result);
            }

            return new PageTitle(null, random.getString("title"), site);
        } catch (Exception e) {
            return null;
        }
    }
}