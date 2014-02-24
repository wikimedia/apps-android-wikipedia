package org.wikipedia.editing;

import android.content.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;


public class EditPreviewTask extends ApiTask<String> {
    private final String wikiText;
    private final PageTitle title;

    public EditPreviewTask(Context context, String wikiText, PageTitle title) {
        super(
                ExecutorService.getSingleton().getExecutor(EditPreviewTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
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
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post();
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        return result.asObject().optJSONObject("parse").optJSONObject("text").optString("*");
    }
}
