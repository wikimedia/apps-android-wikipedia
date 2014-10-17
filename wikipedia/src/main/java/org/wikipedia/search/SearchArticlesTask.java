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
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class SearchArticlesTask extends ApiTask<List<PageTitle>> {
    private final String prefix;
    private final Site site;

    private static final String NUM_RESULTS_PER_QUERY = "12";

    public SearchArticlesTask(Context context, Api api, Site site, String prefix) {
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
                .param("prop", "pageimages")
                .param("piprop", "thumbnail")
                .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE_SEARCH))
                .param("pilimit", NUM_RESULTS_PER_QUERY)
                .param("list", "prefixsearch")
                .param("pssearch", prefix)
                .param("pslimit", NUM_RESULTS_PER_QUERY);
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

        /*
        So here's what we're doing here:
        We're requesting two sets of results with our API query. They both contain the same titles,
        but in different orders.  The results given by "list=prefixsearch" give us the results in
        the correct order, but with no thumbnails.  The results given by "generator=prefixsearch"
        give the results in the wrong order, but with thumbnails!  So, all we have to do is use the
        first list, and correlate the pageids with the second list to extract the thumbnails.
        */
        JSONObject query = data.optJSONObject("query");
        JSONObject pages = query.getJSONObject("pages");
        JSONArray prefixsearch = query.getJSONArray("prefixsearch");

        for (int i = 0; i < prefixsearch.length(); i++) {
            String thumbUrl = null;
            JSONObject item = prefixsearch.getJSONObject(i);
            String pageid = item.getString("pageid");
            if (pages.has(pageid) && pages.getJSONObject(pageid).has("thumbnail")) {
                thumbUrl = pages.getJSONObject(pageid)
                                .getJSONObject("thumbnail").getString("source");
            }
            pageTitles.add(new PageTitle(item.getString("title"), site, thumbUrl));
        }

        return pageTitles;
    }
}
