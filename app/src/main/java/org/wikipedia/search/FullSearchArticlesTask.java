package org.wikipedia.search;

import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.ApiTask;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO: Delete this and its tests after SuggestionsTask is converted to a Retrofit client
@Deprecated public class FullSearchArticlesTask extends ApiTask<SearchResults> {
    private final WikiSite wiki;
    private final String searchTerm;
    private final int maxResults;
    private final Map<String, String> continuation;
    private final boolean getMoreLike;

    public FullSearchArticlesTask(Api api, WikiSite wiki, String searchTerm, int maxResults,
                                  Map<String, String> continuation,
                                  boolean getMoreLike) {
        super(api);
        this.wiki = wiki;
        this.searchTerm = searchTerm;
        this.maxResults = maxResults;
        this.continuation = continuation;
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
                .param("pilicense", "any")
                .param("pithumbsize", Integer.toString(Constants.PREFERRED_THUMB_SIZE))
                .param("pilimit", maxResultsString);
        if (continuation != null) {
            req.param("continue", continuation.get("continue"));
            String gsrOffset = continuation.get("gsroffset");
            if (Integer.parseInt(gsrOffset) > 0) {
                req.param("gsroffset", gsrOffset);
            }
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
                throw e;
            }
        }

        Map<String, String> nextContinueOffset = null;
        final JSONObject continueData = data.optJSONObject("continue");
        if (continueData != null) {
            nextContinueOffset = GsonUnmarshaller.unmarshal(new TypeToken<Map<String, String>>(){},
                    continueData.toString());
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
                    description = arr.getString(0);
                }
            }
            PageProperties properties = null;
            if (item.has("pageprops")) {
                properties = new PageProperties(item.getJSONObject("pageprops"));
            }
            resultList.add(new SearchResult(new PageTitle(item.getString("title"), wiki, thumbUrl, description, properties)));
        }
        return new SearchResults(resultList, nextContinueOffset, null);
    }
}
