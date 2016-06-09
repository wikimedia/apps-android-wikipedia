package org.wikipedia.search;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FullSearchArticlesTask extends ApiTask<SearchResults> {
    private final Site site;
    private final String searchTerm;
    private final int maxResults;
    private final FTContinueOffset continueOffset;
    private final boolean getMoreLike;

    public FullSearchArticlesTask(Api api, Site site, String searchTerm, int maxResults,
                                  SearchResults.ContinueOffset continueOffset,
                                  boolean getMoreLike) {
        super(api);
        this.site = site;
        this.searchTerm = searchTerm;
        this.maxResults = maxResults;
        this.continueOffset = (FTContinueOffset) continueOffset;
        this.getMoreLike = getMoreLike;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        final String maxResultsString = Integer.toString(maxResults);
        final RequestBuilder req = api.action("query")
                .param("prop", "pageterms|pageimages|pageprops")
                .param("ppprop", "mainpage|disambiguation")
                .param("wbptterms", "description") // only interested in Wikidata description
                .param("generator", "search")
                .param("gsrsearch", getMoreLike ? ("morelike:" + searchTerm) : searchTerm)
                .param("gsrnamespace", "0")
                .param("gsrwhat", "text")
                .param("gsrinfo", "")
                .param("gsrprop", "redirecttitle")
                .param("gsrlimit", maxResultsString)
                .param("piprop", "thumbnail") // for thumbnail URLs
                .param("pithumbsize", Integer.toString(Constants.PREFERRED_THUMB_SIZE))
                .param("pilimit", maxResultsString);
        if (continueOffset != null) {
            req.param("continue", continueOffset.cont);
            if (continueOffset.gsroffset > 0) {
                req.param("gsroffset", Integer.toString(continueOffset.gsroffset));
            }
        } else {
            req.param("continue", ""); // add empty continue to avoid the API warning
        }
        return req;
    }

    @Override
    public SearchResults processResult(final ApiResult result) throws Throwable {
        JSONObject data;
        try {
            data = result.asObject();
        } catch (ApiException e) {
            if (e.getCause() instanceof JSONException) {
                // the only reason for a JSONException is if the response is an empty array.
                return new SearchResults();
            } else {
                throw new RuntimeException(e);
            }
        }

        FTContinueOffset nextContinueOffset = null;
        final JSONObject continueData = data.optJSONObject("continue");
        if (continueData != null) {
            String continueString = continueData.optString("continue", null);
            Integer gsroffset = continueData.optInt("gsroffset");
            nextContinueOffset = new FTContinueOffset(continueString, gsroffset);
        }

        JSONObject queryResult = data.optJSONObject("query");
        if (queryResult == null) {
            return new SearchResults();
        }

        JSONObject pages = queryResult.optJSONObject("pages");
        if (pages == null) {
            return new SearchResults();
        }

        // The search results arrive unordered, but they do have an "index" property, which we'll
        // use to sort the results ourselves.
        // First, put all the page objects into an array
        JSONObject[] pageArray = new JSONObject[pages.length()];
        int pageIndex = 0;
        Iterator<String> pageIter = pages.keys();
        while (pageIter.hasNext()) {
            pageArray[pageIndex++] = (JSONObject)pages.get(pageIter.next());
        }
        // now sort the array based on the "index" property
        Arrays.sort(pageArray, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject lhs, JSONObject rhs) {
                int ret = 0;
                try {
                    ret = ((Integer) lhs.getInt("index")).compareTo(rhs.getInt("index"));
                } catch (JSONException e) {
                    //doesn't matter
                }
                return ret;
            }
        });
        // and create our list of results from the now-sorted array
        List<SearchResult> resultList = new ArrayList<>();
        for (JSONObject item : pageArray) {
            String thumbUrl = null;
            if (item.has("thumbnail")) {
                thumbUrl = item.getJSONObject("thumbnail").optString("source", null);
            }
            String description = null;
            if (item.has("terms")) {
                JSONArray arr = item.getJSONObject("terms").optJSONArray("description");
                if (arr != null && arr.length() > 0) {
                    description = StringUtil.capitalizeFirstChar(arr.getString(0));
                }
            }
            PageProperties properties = null;
            if (item.has("pageprops")) {
                properties = new PageProperties(item.getJSONObject("pageprops"));
            }
            resultList.add(new SearchResult(new PageTitle(item.getString("title"), site, thumbUrl, description, properties)));
        }
        return new SearchResults(resultList, nextContinueOffset, null);
    }

    public final class FTContinueOffset extends SearchResults.ContinueOffset {
        private String cont;
        private int gsroffset;

        private FTContinueOffset(String cont, int gsroffset) {
            this.cont = cont;
            this.gsroffset = gsroffset;
        }
    }
}
