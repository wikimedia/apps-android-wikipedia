package org.wikipedia.editing;

import android.content.Context;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;


public class EditPreviewTask extends ApiTask<String> {
    private final String wikiText;
    private final PageTitle title;

    public EditPreviewTask(Context context, String wikiText, PageTitle title) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite()));
        this.wikiText = wikiText;
        this.title = title;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("parse")
                .param("sectionpreview", "true")
                .param("pst", "true")
                .param("mobileformat", "true")
                .param("prop", "text")
                .param("title", title.getPrefixedText())
                .param("text", wikiText);
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post();
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        return result.asObject().optJSONObject("parse").optJSONObject("text").optString("*");
    }
}
