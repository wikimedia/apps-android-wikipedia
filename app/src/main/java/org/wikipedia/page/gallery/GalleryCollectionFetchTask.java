package org.wikipedia.page.gallery;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;

import java.util.Map;

public abstract class GalleryCollectionFetchTask extends PageQueryTask<GalleryItem> {
    private static final String MAX_ITEM_COUNT = "256";
    private final boolean getThumbs;

    public GalleryCollectionFetchTask(Api api, Site site, PageTitle title) {
        this(api, site, title, false);
    }

    public GalleryCollectionFetchTask(Api api, Site site, PageTitle title, boolean getThumbs) {
        super(api, site, title);
        this.getThumbs = getThumbs;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "imageinfo")
               .param("iiprop", getThumbs ? "dimensions|mime|url" : "dimensions|mime")
               .param("generator", "images")
               .param("redirects", "")
               .param("gimlimit", MAX_ITEM_COUNT);
        // If we've been asked to retrieve the thumbnail url for each of the images, then
        // we need to specify the width and height of the thumbnails. Otherwise, we would
        // just get the name of each image, and we would need to send a separate request
        // to get the url (and other info) for the image.
        if (getThumbs) {
            builder.param("iiurlwidth", Integer.toString(Constants.PREFERRED_THUMB_SIZE))
                    .param("iiurlheight", Integer.toString(Constants.PREFERRED_THUMB_SIZE));
        }
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

    public abstract void onGalleryResult(GalleryCollection result);
}
