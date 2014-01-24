package org.wikipedia.random;

import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;
import android.content.Context;

public class RandomArticleIdTask extends ApiTask<String> {

    private Context ctx;

    public RandomArticleIdTask(Api api, Context context) {
        super(ExecutorService.getSingleton().getExecutor(RandomArticleIdTask.class, 1), api);
        this.ctx = context;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("list", "random")
                .param("rnlimit", "1"); // maybe we grab 10 in the future and persist it somewhere
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        try {
            JSONArray results = result.asObject().optJSONObject("query").optJSONArray("random");
            JSONObject random = (JSONObject)results.get(0);

            if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
                Utils.processHeadersForZero((WikipediaApp)ctx, result);
            }

            return random.getString("title");
        } catch (Exception e) {
            return null;
        }
    }
}