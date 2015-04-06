package org.wikipedia.page.linkpreview;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.PageQueryTask;
import org.wikipedia.PageTitle;

public class PreviewFetchTask extends PageQueryTask<LinkPreviewContents> {
    private final PageTitle title;

    public PreviewFetchTask(Api api, PageTitle title) {
        super(LOW_CONCURRENCY, api, title.getSite(), title);
        this.title = title;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        // instead of requesting a certain number of sentences from the server ("exsentences"),
        // we'll request a certain number of characters ("exchars") and break the text into
        // sentences ourselves, since we can do it locally with a BreakIterator using the
        // appropriate locale (produces better results than what the server provides currently).
        // TODO: implement better sentence parsing in our future web service.
        builder.param("prop", "extracts|pageimages|pageterms")
               .param("redirects", "true")
               .param("exchars", "512")
               .param("explaintext", "true")
               .param("piprop", "thumbnail")
               .param("pithumbsize", "640")
               .param("wbptterms", "description");
    }

    @Override
    public LinkPreviewContents processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new LinkPreviewContents(pageData, title.getSite());
    }
}
