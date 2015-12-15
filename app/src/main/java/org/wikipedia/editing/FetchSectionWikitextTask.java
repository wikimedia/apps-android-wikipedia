package org.wikipedia.editing;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class FetchSectionWikitextTask extends ApiTask<String> {
    private final PageTitle title;
    private final int sectionID;

    public FetchSectionWikitextTask(Context context, PageTitle title, int sectionID) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite()));
        this.title = title;
        this.sectionID = sectionID;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "revisions")
                .param("rvprop", "content")
                .param("rvlimit", "1")
                .param("titles", title.getPrefixedText())
                .param("rvsection", String.valueOf(sectionID));
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        JSONObject pagesJSON = result.asObject()
                .optJSONObject("query")
                .optJSONObject("pages");
        String pageId = pagesJSON.keys().next();

        JSONObject revisionJSON = pagesJSON.optJSONObject(pageId).optJSONArray("revisions").getJSONObject(0);

        return revisionJSON.optString("*");
    }
}
