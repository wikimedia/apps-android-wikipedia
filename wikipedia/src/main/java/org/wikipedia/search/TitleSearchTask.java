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
import java.util.List;

public class TitleSearchTask extends ApiTask<List<PageTitle>> {
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
                .param("gpssearch", prefix)
                .param("gpsnamespace", "0")
                .param("gpslimit", NUM_RESULTS_PER_QUERY)
                .param("prop", "pageterms|pageimages")
                .param("wbptterms", "description") // only interested in Wikidata description
                .param("piprop", "thumbnail")
                .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE))
                .param("pilimit", NUM_RESULTS_PER_QUERY)
                .param("continue", ""); // to avoid warning about new continuation syntax
    }

    @Override
    public List<PageTitle> processResult(final ApiResult result) throws Throwable {
        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        JSONObject data;
        try {
            data = result.asObject();
        } catch (ApiException e) {
            if (e.getCause() instanceof JSONException) {
                // the only reason for a JSONException is if the response is an empty array.
                return pageTitles;
            } else {
                throw new RuntimeException(e);
            }
        }

        // The search results arrive unordered, but they do have an "index" property, which we'll
        // use to sort the results ourselves.
        JSONObject queryResult = data.optJSONObject("query");
        if (queryResult == null) {
            return pageTitles;
        }
        JSONObject pages = queryResult.optJSONObject("pages");
        if (pages == null) {
            return pageTitles;
        }
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
        return pageTitles;
    }
}
