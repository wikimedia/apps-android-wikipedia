package org.wikipedia.search;

import org.json.JSONArray;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class TitleSearchTask extends ApiTask<SearchResults> {
    private final String prefix;
    private final Site site;

    private static final String NUM_RESULTS_PER_QUERY = "20";

    public TitleSearchTask(Api api, Site site, String prefix) {
        super(HIGH_CONCURRENCY, api);
        this.prefix = prefix;
        this.site = site;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("generator", "prefixsearch")
                .param("redirects", "true")
                .param("gpssearch", prefix)
                .param("gpsnamespace", "0")
                .param("gpslimit", NUM_RESULTS_PER_QUERY)
                // -- Parameters causing prefix search to return suggestion.
                .param("list", "search")
                .param("srsearch", prefix)
                .param("srnamespace", "0")
                .param("srwhat", "text")
                .param("srinfo", "suggestion")
                .param("srprop", "")
                .param("sroffset", "0")
                .param("srlimit", "1")
                // --
                .param("prop", "pageterms|pageimages")
                .param("wbptterms", "description") // only interested in Wikidata description
                .param("piprop", "thumbnail")
                .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE))
                .param("pilimit", NUM_RESULTS_PER_QUERY)
                .param("continue", ""); // to avoid warning about new continuation syntax
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

        // The search results arrive unordered, but they do have an "index" property, which we'll
        // use to sort the results ourselves.
        JSONObject queryResult = data.optJSONObject("query");
        if (queryResult == null) {
            return new SearchResults();
        }

        String suggestion = "";
        JSONObject searchinfo = queryResult.optJSONObject("searchinfo");
        if (searchinfo != null) {
            if (searchinfo.has("suggestion")) {
                suggestion = searchinfo.getString("suggestion");
            }
        }
        ArrayList<PageTitle> pageTitles = new ArrayList<>();
        JSONObject pages = queryResult.optJSONObject("pages");
        if (pages == null) {
            return new SearchResults(pageTitles, null, suggestion);
        }

        // Collect a list of redirect targets, if available.
        // This provides us with the order in which the redirects are listed in the results,
        // since the redirected results don't come with an "index" property.
        ArrayList<String> redirectTargetList = new ArrayList<>();
        if (queryResult.has("redirects")) {
            JSONArray redirs = queryResult.getJSONArray("redirects");
            for (int i = 0; i < redirs.length(); i++) {
                redirectTargetList.add(((JSONObject) redirs.get(i)).getString("to"));
            }
        }

        // Create a list of indices, which will be claimed by results that have an "index".
        // Results that are redirects will not have an "index", so we will manually place them
        // into any indices that are left over.
        ArrayList<Integer> pageIndices = new ArrayList<>();
        for (int i = 0; i < pages.length(); i++) {
            pageIndices.add(i + 1);
        }

        // First, put all the page objects into an array
        JSONObject[] pageArray = new JSONObject[pages.length()];
        int pageIndex = 0;
        Iterator<String> pageIter = pages.keys();
        while (pageIter.hasNext()) {
            JSONObject page = (JSONObject)pages.get(pageIter.next());
            pageArray[pageIndex++] = page;
            if (page.has("index")) {
                pageIndices.remove((Integer) page.getInt("index"));
            }
        }
        // add an index to any results that didn't have one, in the order that they appear
        // in the redirect map.
        for (String redirTo : redirectTargetList) {
            for (JSONObject page : pageArray) {
                if (page.getString("title").equals(redirTo)
                    && !page.has("index") && pageIndices.size() > 0) {
                    page.put("index", pageIndices.get(0));
                    pageIndices.remove(0);
                }
            }
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
        // and create our list of PageTitles from the now-sorted array
        for (JSONObject item : pageArray) {
            String thumbUrl = null;
            if (item.has("thumbnail")) {
                thumbUrl = item.getJSONObject("thumbnail").optString("source", null);
            }
            String description = null;
            if (item.has("terms")) {
                JSONArray arr = item.getJSONObject("terms").optJSONArray("description");
                if (arr != null && arr.length() > 0) {
                    description = Utils.capitalizeFirstChar(arr.getString(0));
                }
            }
            pageTitles.add(new PageTitle(item.getString("title"), site, thumbUrl, description));
        }
        return new SearchResults(pageTitles, null, suggestion);
    }
}
