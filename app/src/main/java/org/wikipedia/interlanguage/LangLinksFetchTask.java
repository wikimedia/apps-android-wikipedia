package org.wikipedia.interlanguage;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;

public class LangLinksFetchTask extends ApiTask<ArrayList<PageTitle>> {
    private final PageTitle title;
    public LangLinksFetchTask(Context context, PageTitle title) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite()));
        this.title = title;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "langlinks")
                .param("titles", title.getPrefixedText())
                .param("lllimit", "500")
                .param("continue", ""); // to avoid warning about new continuation syntax
    }

    @Override
    public ArrayList<PageTitle> processResult(ApiResult result) throws Throwable {
        ArrayList<PageTitle> linkTitles = new ArrayList<>();
        JSONObject pagesJSON = result.asObject()
                .optJSONObject("query")
                .optJSONObject("pages");
        String pageId = pagesJSON.keys().next();
        if (!pagesJSON.optJSONObject(pageId).has("langlinks")) {
            // No links found
            return linkTitles;
        }

        JSONArray langlinksJSON = pagesJSON.optJSONObject(pageId).optJSONArray("langlinks");

        for (int i = 0; i < langlinksJSON.length(); i++) {
            JSONObject langlinkJSON = langlinksJSON.optJSONObject(i);
            PageTitle linkTitle = new PageTitle(
                    langlinkJSON.optString("*"),
                    Site.forLanguageCode(langlinkJSON.optString("lang")));
            linkTitles.add(linkTitle);
        }

        return linkTitles;
    }
}
