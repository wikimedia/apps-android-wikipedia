package org.wikimedia.wikipedia;

import android.content.Context;
import org.json.JSONArray;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;

import java.util.ArrayList;
import java.util.List;

public class SearchArticlesTask extends ApiTask<List<PageTitle>>{
    private String prefix;

    public SearchArticlesTask(Context context, Site site, String prefix) {
        super(context, site);
        this.prefix = prefix;
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
            pageTitles.add(new PageTitle(null, searchResults.optString(i), getSite()));
        }

        return pageTitles;
    }
}
