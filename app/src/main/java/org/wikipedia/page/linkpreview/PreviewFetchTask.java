package org.wikipedia.page.linkpreview;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;

public class PreviewFetchTask extends PageQueryTask<LinkPreviewContents> {
    private final PageTitle title;

    public PreviewFetchTask(Api api, PageTitle title) {
        super(LOW_CONCURRENCY, api, title.getSite(), title);
        this.title = title;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "extracts|pageimages|pageterms")
               .param("redirects", "true")
               .param("exchars", "512")
               //.param("exsentences", "2")
               .param("explaintext", "true")
               .param("piprop", "thumbnail|name")
               .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE))
               .param("wbptterms", "description");
    }

    @Override
    public LinkPreviewContents processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new LinkPreviewContents(pageData, title.getSite());
    }
}
