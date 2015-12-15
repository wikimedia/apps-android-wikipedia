package org.wikipedia.page.gallery;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;

public class GalleryItemFetchTask extends PageQueryTask<GalleryItem> {
    private static final String MAX_IMAGE_WIDTH = "1280";
    private final boolean isVideo;

    public GalleryItemFetchTask(Api api, Site site, PageTitle title, boolean isVideo) {
        super(api, site, title);
        this.isVideo = isVideo;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        if (isVideo) {
            builder.param("prop", "videoinfo")
                   .param("viprop", "url|dimensions|mime|extmetadata|derivatives")
                   .param("viurlwidth", MAX_IMAGE_WIDTH);
        } else {
            builder.param("prop", "imageinfo")
                   .param("iiprop", "url|dimensions|mime|extmetadata")
                   .param("iiurlwidth", MAX_IMAGE_WIDTH);
        }
    }

    @Override
    public GalleryItem processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new GalleryItem(pageData);
    }
}
