package org.wikipedia.search;

import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FullSearchArticlesTask extends ApiTask<FullSearchArticlesTask.FullSearchResults> {
    private final Site site;
    private final String searchTerm;
    private final int maxResults;
    private final ContinueOffset continueOffset;

    public FullSearchArticlesTask(Api api, Site site, String searchTerm, int maxResults, ContinueOffset continueOffset) {
        super(LOW_CONCURRENCY, api);
        this.site = site;
        this.searchTerm = searchTerm;
        this.maxResults = maxResults;
        this.continueOffset = continueOffset;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        final String maxResultsString = Integer.toString(maxResults);
        final RequestBuilder req = api.action("query")
                .param("prop", "pageprops|pageimages")
                .param("ppprop", "wikibase_item") // only interested in wikibase_item
                .param("generator", "search")
                .param("gsrsearch", searchTerm)
                .param("gsrnamespace", "0")
                .param("gsrwhat", "text")
                .param("gsrinfo", "")
                .param("gsrprop", "redirecttitle")
                .param("gsrlimit", maxResultsString)
                .param("list", "search") // for correct order
                .param("srsearch", searchTerm)
                .param("srnamespace", "0")
                .param("srwhat", "text")
                .param("srinfo", "suggestion")
                .param("srprop", "")
                .param("srlimit", maxResultsString)
                .param("piprop", "thumbnail") // for thumbnail URLs
                .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE))
                .param("pilimit", maxResultsString);
        if (continueOffset != null) {
            req.param("continue", continueOffset.cont);
            if (continueOffset.sroffset > 0) {
                req.param("sroffset", Integer.toString(continueOffset.sroffset));
            }
            if (continueOffset.gsroffset > 0) {
                req.param("gsroffset", Integer.toString(continueOffset.gsroffset));
            }
        } else {
            req.param("continue", ""); // add empty continue to avoid the API warning
        }
        return req;
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

        ContinueOffset nextContinueOffset = null;
        final JSONObject continueData = data.optJSONObject("continue");
        if (continueData != null) {
            String continueString = continueData.optString("continue", null);
            Integer sroffset = continueData.optInt("sroffset");
            Integer gsroffset = continueData.optInt("gsroffset");
            nextContinueOffset = new ContinueOffset(continueString, sroffset, gsroffset);
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

        /*
        So here's what we're doing here:
        We're requesting two sets of results with our API query. They both contain the same titles,
        but in different orders.  The results given by "list=search" give us the results in
        the correct order, but with no thumbnails or wikidata ID. The results given by "generator=search"
        give the results in the wrong order, but with thumbnails and wikidata IDs!
        So, all we have to do is use the first list, and correlate the titles with the second list to
        extract the thumbnails. Unfortunately, the search generator only gives us titles and not pageids.
        This is why we need a Map of titles to results.
        */

        // build a map of full result objects
        Map<String, FullSearchResult> map = new HashMap<String, FullSearchResult>(maxResults + 1, 1.0f);
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
            String thumbUrl = null;
            if (pageData.has("thumbnail")) {
                JSONObject thumbnail = pageData.getJSONObject("thumbnail");
                thumbUrl = thumbnail.optString("source", null);
            }

            map.put(titleString, new FullSearchResult(pageTitle, thumbUrl, wikiBaseId));
        }

        // put them into the list in the correct order
        ArrayList<FullSearchResult> resultList = new ArrayList<FullSearchResult>();
        JSONArray search = queryResult.getJSONArray("search");
        for (int i = 0; i < search.length(); i++) {
            final FullSearchResult res = map.get(search.getJSONObject(i).getString("title"));
            if (res != null) {
                resultList.add(res);
            }
        }

        return new FullSearchResults(resultList, nextContinueOffset, suggestion);
    }

    private FullSearchResults emptyResults() {
        return new FullSearchResults(Collections.<FullSearchResult>emptyList(), null, "");
    }

    public class FullSearchResults {
        private ContinueOffset continueOffset;
        private List<FullSearchResult> resultsList;
        private String suggestion;

        public FullSearchResults(List<FullSearchResult> resultList,
                                 ContinueOffset continueOffset,
                                 String suggestion) {
            this.resultsList = resultList;
            this.continueOffset = continueOffset;
            this.suggestion = suggestion;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public ContinueOffset getContinueOffset() {
            return continueOffset;
        }

        public List<FullSearchResult> getResults() {
            return resultsList;
        }
    }

    public class ContinueOffset {
        private String cont;
        private int sroffset;
        private int gsroffset;

        private ContinueOffset(String cont, int sroffset, int gsroffset) {
            this.cont = cont;
            this.sroffset = sroffset;
            this.gsroffset = gsroffset;
        }
    }
}
