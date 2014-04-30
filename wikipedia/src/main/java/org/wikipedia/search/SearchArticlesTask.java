package org.wikipedia.search;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

import java.util.*;

public class SearchArticlesTask extends ApiTask<List<PageTitle>> {
    private final String prefix;
    private final Site site;
    private final WikipediaApp app;

    public SearchArticlesTask(Context context, Api api, Site site, String prefix) {
        super(HIGH_CONCURRENCY, api);
        this.prefix = prefix;
        this.site = site;
        this.app = (WikipediaApp)context.getApplicationContext();

    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("opensearch").param("search", prefix).param("limit", "12");
    }

    @Override
    public List<PageTitle> processResult(final ApiResult result) throws Throwable {
        JSONArray searchResults = result.asArray().optJSONArray(1);

        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        for (int i = 0; i < searchResults.length(); i++) {
            pageTitles.add(new PageTitle(searchResults.optString(i), site));
        }

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            Utils.processHeadersForZero(app, result);
        }
        return pageTitles;
    }
}
