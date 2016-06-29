package org.wikipedia.search;

import org.json.JSONArray;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
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
import java.util.List;

import static org.wikipedia.util.StringUtil.capitalizeFirstChar;

public class TitleSearchTask extends ApiTask<SearchResults> {
    private final String prefix;
    private final Site site;

    private static final String NUM_RESULTS_PER_QUERY = "20";

    public TitleSearchTask(Api api, Site site, String prefix) {
        super(api);
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
                .param("pithumbsize", Integer.toString(Constants.PREFERRED_THUMB_SIZE))
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
        List<SearchResult> resultList = new ArrayList<>();
        JSONObject pages = queryResult.optJSONObject("pages");
        if (pages == null) {
            return new SearchResults(resultList, null, suggestion);
        }

        // First, put all the page objects into an array
        JSONObject[] pageArray = new JSONObject[pages.length()];
        int pageIndex = 0;
        Iterator<String> pageIter = pages.keys();
        while (pageIter.hasNext()) {
            JSONObject page = (JSONObject)pages.get(pageIter.next());
            pageArray[pageIndex++] = page;
        }

        // Go through the redirects array (if any), and transfer any "tofragment" items to
        // the original results. "tofragment", if it exists, contains a link to a particular
        // section of the page, which is important to preserve so the user gets redirected to the
        // correct section.
        if (queryResult.has("redirects")) {
            JSONArray redirs = queryResult.getJSONArray("redirects");
            for (int i = 0; i < redirs.length(); i++) {
                JSONObject redirect = (JSONObject) redirs.get(i);
                for (JSONObject page : pageArray) {
                    if (page.getString("title").equals(redirect.getString("to"))) {
                        page.put("redirectFrom", redirect.optString("from"));
                        if (redirect.has("tofragment") && !page.has("tofragment")) {
                            page.put("tofragment", redirect.getString("tofragment"));
                        }
                    }
                }
            }
        }

        // now sort the array based on the "index" property
        Arrays.sort(pageArray, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject lhs, JSONObject rhs) {
                return ((Integer) lhs.optInt("index")).compareTo(rhs.optInt("index"));
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
                    description = capitalizeFirstChar(arr.getString(0));
                }
            }
            String titleText = item.getString("title");
            if (item.has("tofragment")) {
                titleText += "#" + item.getString("tofragment");
            }
            resultList.add(new SearchResult(new PageTitle(titleText, site, thumbUrl, description),
                    item.optString("redirectFrom")));
        }
        return new SearchResults(resultList, null, suggestion);
    }
}
