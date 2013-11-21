package org.wikimedia.wikipedia;

import android.content.Context;
import org.json.JSONArray;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikimedia.wikipedia.concurrency.ExecutorService;

import java.util.ArrayList;
import java.util.List;

public class SearchArticlesTask extends ApiTask<List<PageTitle>>{
    private final String prefix;
    private final Site site;

    public SearchArticlesTask(Api api, Site site, String prefix) {
        super(ExecutorService.getSingleton().getExecutor(SearchArticlesTask.class, 2), api);
        this.prefix = prefix;
        this.site = site;
    }

    @Override
    public ApiResult buildRequest(Api api) {
        return api.action("opensearch").param("search", prefix).param("limit", "5").get();
    }

    @Override
    public List<PageTitle> processResult(ApiResult result) throws Throwable {
        JSONArray searchResults = result.asArray().optJSONArray(1);

        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        for (int i = 0; i < searchResults.length(); i++) {
            pageTitles.add(new PageTitle(null, searchResults.optString(i), site));
        }

        return pageTitles;
    }
}
