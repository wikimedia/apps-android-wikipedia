package org.wikimedia.wikipedia;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.RequestBuilder;

import java.util.List;

public class PageImagesTask extends PageQueryTask<String> {
    private int thumbSize;
    private int maxThumbs;

    public PageImagesTask(Context context, Site site, List<PageTitle> titles, int thumbSize) {
        super(context, site, titles);
        this.thumbSize = thumbSize;
        maxThumbs = titles.size();
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageimages")
               .param("piprop", "thumbnail")
               .param("pithumbsize", Integer.toString(thumbSize))
               .param("pilimit", Integer.toString(maxThumbs));
    }

    @Override
    public String processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        JSONObject thumbnail = pageData.optJSONObject("thumbnail");
        if (thumbnail == null) {
            return null;
        } else {
            return thumbnail.getString("source");
        }
    }
}
