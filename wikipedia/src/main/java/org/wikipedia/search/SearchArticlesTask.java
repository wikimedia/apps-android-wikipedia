package org.wikipedia.search;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.*;

public class SearchArticlesTask extends ApiTask<List<PageTitle>> {
    private final String prefix;
    private final Site site;
    private final WikipediaApp app;

    public SearchArticlesTask(Context context, Api api, Site site, String prefix) {
        super(HIGH_CONCURRENCY, api);
        this.prefix = prefix;
        this.site = site;
        this.app = (WikipediaApp)context.getApplicationContext();

    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("generator", "prefixsearch")
                .param("gpssearch", prefix)
                .param("gpsnamespace", "0")
                .param("gpslimit", "12")
                .param("prop", "pageimages")
                .param("piprop", "thumbnail")
                .param("pithumbsize", "48")
                .param("pilimit", "12");
    }

    @Override
    public List<PageTitle> processResult(final ApiResult result) throws Throwable {
        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        JSONObject data = null;
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
        JSONObject query = data.optJSONObject("query");
        JSONObject pages = query.getJSONObject("pages");

        Iterator<String> keys = pages.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject page = pages.getJSONObject(key);
            String thumbUrl = null;
            if (page.has("thumbnail")) {
                thumbUrl = page.getJSONObject("thumbnail").getString("source");
            }
            pageTitles.add(new PageTitle(page.getString("title"), site, thumbUrl));
        }

        Collections.sort(pageTitles, new Comparator<PageTitle>(){
            @Override
            public int compare(PageTitle pageTitle, PageTitle pageTitle2) {
                return pageTitle.getDisplayText().compareTo(pageTitle2.getDisplayText());
            }
        });

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            Utils.processHeadersForZero(app, result);
        }
        return pageTitles;
    }
}
