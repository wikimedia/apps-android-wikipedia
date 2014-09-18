package org.wikipedia.beta.interlanguage;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.ApiTask;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;

import java.util.ArrayList;

public class LangLinksFetchTask extends ApiTask<ArrayList<PageTitle>> {
    private final PageTitle title;
    private final WikipediaApp app;
    public LangLinksFetchTask(Context context, PageTitle title) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.app = (WikipediaApp)context.getApplicationContext();
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "langlinks")
                .param("titles", title.getPrefixedText())
                .param("lllimit", "500");
    }

    @Override
    public ArrayList<PageTitle> processResult(ApiResult result) throws Throwable {
        ArrayList<PageTitle> linkTitles = new ArrayList<PageTitle>();
        JSONObject pagesJSON = result.asObject()
                .optJSONObject("query")
                .optJSONObject("pages");
        String pageId = (String) pagesJSON.keys().next();
        if (!pagesJSON.optJSONObject(pageId).has("langlinks")) {
            // No links found
            if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
                Utils.processHeadersForZero(app, result);
            }
            return linkTitles;
        }

        JSONArray langlinksJSON = pagesJSON.optJSONObject(pageId).optJSONArray("langlinks");

        for (int i = 0; i < langlinksJSON.length(); i++) {
            JSONObject langlinkJSON = langlinksJSON.optJSONObject(i);
            PageTitle linkTitle = new PageTitle(
                    langlinkJSON.optString("*"),
                    Site.forLang(langlinkJSON.optString("lang")));
            linkTitles.add(linkTitle);
        }
        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            // As with page edit text retrieval, the next or calling activity
            // will reflect the side effect of the header processing. It seems
            // having a bus in the calling activity may not make much sense.
            // TODO: ??? add bus to calling activity?
            Utils.processHeadersForZero(app, result);
        }

        return linkTitles;
    }
}
