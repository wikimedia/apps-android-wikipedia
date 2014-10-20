package org.wikipedia.search;

import org.wikipedia.ApiTask;
import org.wikipedia.Site;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class FullSearchArticlesTask extends ApiTask<FullSearchArticlesTask.FullSearchResults> {
    private final String searchTerm;
    private int continueOffset;

    public FullSearchArticlesTask(Context context, Api api, Site site, String searchTerm, int continueOffset) {
        super(LOW_CONCURRENCY, api);
        this.searchTerm = searchTerm;
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

        String suggestion = "";
        JSONObject searchinfo = queryResult.optJSONObject("searchinfo");
        if (searchinfo != null) {
            if (searchinfo.has("suggestion")) {
                suggestion = searchinfo.getString("suggestion");
            }
        }

        int newOffset = 0;
        if (data.has("query-continue")) {
            newOffset = data.getJSONObject("query-continue").getJSONObject("search").getInt("sroffset");
        }

        ArrayList<FullSearchResult> resultList = new ArrayList<FullSearchResult>();
        FullSearchResults results = new FullSearchResults(resultList, newOffset, suggestion);
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

        return results;
    }

    public class FullSearchResults {
        private int continueOffset;
        private List<FullSearchResult> resultsList;
        private String suggestion;

        public FullSearchResults(List<FullSearchResult> resultList,
                                 int continueOffset,
                                 String suggestion) {
            this.resultsList = resultList;
            this.continueOffset = continueOffset;
            this.suggestion = suggestion;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public int getContinueOffset() {
            return continueOffset;
        }

        public List<FullSearchResult> getResults() {
            return resultsList;
        }
    }
}
