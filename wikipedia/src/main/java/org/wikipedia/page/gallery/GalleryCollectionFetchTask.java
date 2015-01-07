package org.wikipedia.page.gallery;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.PageQueryTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import java.util.Map;

public abstract class GalleryCollectionFetchTask extends PageQueryTask<GalleryItem> {
    private static final String MAX_ITEM_COUNT = "256";

    public GalleryCollectionFetchTask(Api api, Site site, PageTitle title) {
        super(LOW_CONCURRENCY, api, site, title);
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "imageinfo")
               .param("iiprop", "dimensions|mime")
               .param("generator", "images")
               .param("gimlimit", MAX_ITEM_COUNT);
    }

    @Override
    public GalleryItem processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new GalleryItem(pageData);
    }

    @Override
    public void onFinish(Map<PageTitle, GalleryItem> result) {
        GalleryCollection collection = new GalleryCollection(result);
        onGalleryResult(collection);
    }

    abstract void onGalleryResult(GalleryCollection result);
}
