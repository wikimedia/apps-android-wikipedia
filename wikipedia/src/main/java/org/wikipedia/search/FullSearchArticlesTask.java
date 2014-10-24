package org.wikipedia.search;

import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FullSearchArticlesTask extends ApiTask<FullSearchArticlesTask.FullSearchResults> {
    private static final String NUM_RESULTS_PER_QUERY = "12";

    private final Site site;
    private final String searchTerm;
    private final int continueOffset;

    public FullSearchArticlesTask(Api api, Site site, String searchTerm, int continueOffset) {
        super(LOW_CONCURRENCY, api);
        this.site = site;
        this.searchTerm = searchTerm;
        this.continueOffset = continueOffset;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "pageprops")
                .param("ppprop", "wikibase_item") // only interested in wikibase_item
                .param("generator", "search")
                .param("gsrsearch", searchTerm)
                .param("gsrnamespace", "0")
                .param("gsrwhat", "text")
                .param("gsrinfo", "totalhits|suggestion")
                .param("gsrprop", "redirecttitle")
                .param("gsroffset", Integer.toString(continueOffset))
                .param("gsrlimit", NUM_RESULTS_PER_QUERY);
    }

    @Override
    public FullSearchResults processResult(final ApiResult result) throws Throwable {
        JSONObject data;
        try {
            data = result.asObject();
        } catch (ApiException e) {
            if (e.getCause() instanceof JSONException) {
                // the only reason for a JSONException is if the response is an empty array.
                return emptyResults();
            } else {
                throw new RuntimeException(e);
            }
        }

        int newOffset = 0;
        if (data.has("query-continue")) {
            newOffset = data.getJSONObject("query-continue").getJSONObject("search").getInt("gsroffset");
        }

        JSONObject queryResult = data.optJSONObject("query");

        String suggestion = "";
        JSONObject searchinfo = queryResult.optJSONObject("searchinfo");
        if (searchinfo != null) {
            if (searchinfo.has("suggestion")) {
                suggestion = searchinfo.getString("suggestion");
            }
        }

        JSONObject pages = queryResult.optJSONObject("pages");
        if (pages == null) {
            return emptyResults();
        }

        ArrayList<FullSearchResult> resultList = new ArrayList<FullSearchResult>();
        Iterator<String> keys = pages.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject pageData = pages.getJSONObject(key);
            String titleString = pageData.getString("title");
            PageTitle pageTitle = new PageTitle(titleString, site);
            String wikiBaseId = null;
            if (pageData.has("pageprops")) {
                JSONObject pageProps = pageData.getJSONObject("pageprops");
                wikiBaseId = pageProps.optString("wikibase_item", null);
            }
            resultList.add(new FullSearchResult(pageTitle, wikiBaseId));
        }

        return new FullSearchResults(resultList, newOffset, suggestion);
    }

    private FullSearchResults emptyResults() {
        return new FullSearchResults(Collections.<FullSearchResult>emptyList(), 0, "");
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
