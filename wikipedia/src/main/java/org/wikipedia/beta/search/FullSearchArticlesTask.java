package org.wikipedia.beta.search;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.ApiTask;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;

import java.util.ArrayList;
import java.util.List;

public class FullSearchArticlesTask extends ApiTask<FullSearchArticlesTask.FullSearchResults> {
    private final String searchTerm;
    private final Site site;
    private final WikipediaApp app;
    private int continueOffset;

    public FullSearchArticlesTask(Context context, Api api, Site site, String searchTerm, int continueOffset) {
        super(LOW_CONCURRENCY, api);
        this.searchTerm = searchTerm;
        this.site = site;
        this.app = (WikipediaApp)context.getApplicationContext();
        this.continueOffset = continueOffset;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("list", "search")
                .param("srsearch", searchTerm)
                .param("srlimit", "12")
                .param("srinfo", "totalhits|suggestion")
                .param("srprop", "snippet|redirecttitle|redirectsnippet")
                .param("sroffset", Integer.toString(continueOffset));
    }

    @Override
    public FullSearchResults processResult(final ApiResult result) throws Throwable {
        JSONObject data = result.asObject();
        JSONObject queryResult = data.optJSONObject("query");
        JSONArray searchResults = queryResult.optJSONArray("search");

        if (searchResults.length() == 0) {
            String suggestion = queryResult.optJSONObject("searchinfo").optString("suggestion");
            if (suggestion != null && suggestion.length() > 0) {
                throw new FullSearchSuggestionException(suggestion);
            }
        }

        int newOffset = 0;
        if (data.has("query-continue")) {
            newOffset = data.getJSONObject("query-continue").getJSONObject("search").getInt("sroffset");
        }

        ArrayList<FullSearchResult> resultList = new ArrayList<FullSearchResult>();
        FullSearchResults results = new FullSearchResults(resultList, newOffset);
        for (int i = 0; i < searchResults.length(); i++) {
            JSONObject res = searchResults.optJSONObject(i);
            String redirectTitle = "";
            if (res.has("redirecttitle")) {
                redirectTitle = res.getJSONObject("redirecttitle").getString("mUrlform");
            }
            resultList.add(new FullSearchResult(res.optString("title"),
                    res.optString("snippet"),
                    redirectTitle,
                    res.optString("redirectsnippet")));
        }

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            Utils.processHeadersForZero(app, result);
        }

        return results;
    }

    public class FullSearchResults {
        private int continueOffset;
        private List<FullSearchResult> resultsList;

        public FullSearchResults(List<FullSearchResult> resultList, int continueOffset) {
            this.resultsList = resultList;
            this.continueOffset = continueOffset;
        }

        public int getContinueOffset() {
            return continueOffset;
        }

        public List<FullSearchResult> getResults() {
            return resultsList;
        }
    }
}
