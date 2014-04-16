package org.wikipedia.editing;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class FetchSectionWikitextTask extends ApiTask<String> {
    private final PageTitle title;
    private final int sectionID;
    private final WikipediaApp app;

    public FetchSectionWikitextTask(Context context, PageTitle title, int sectionID) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sectionID = sectionID;
        this.app = (WikipediaApp)context.getApplicationContext();
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
        String pageId = (String) pagesJSON.keys().next();

        JSONObject revisionJSON = pagesJSON.optJSONObject(pageId).optJSONArray("revisions").getJSONObject(0);
        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            // No bus is wired in the calling activity. If the state of zero has
            // changed by the time we get here, do we really want to be firing an
            // event at the UI? Or is the side effect of updating state in
            // Utils.processHeadersForZero sufficient, with the high likelihood
            // of the user getting the notification when returning back to the main
            // activity (and if on save it changes yet again, no change in the main activity?
            // TODO: ??? add bus to calling activity? See preceding comment
            Utils.processHeadersForZero(app, result);
        }

        return revisionJSON.optString("*");
    }
}
