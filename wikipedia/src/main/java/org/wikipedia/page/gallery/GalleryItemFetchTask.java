package org.wikipedia.page.gallery;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.PageQueryTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;

public class GalleryItemFetchTask extends PageQueryTask<GalleryItem> {
    public GalleryItemFetchTask(Api api, Site site, PageTitle title) {
        super(LOW_CONCURRENCY, api, site, title);
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "imageinfo")
               .param("iiprop", "url|dimensions|mime|extmetadata")
               .param("iiurlwidth", "2048");
    }

    @Override
    public GalleryItem processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new GalleryItem(pageData);
    }
}
