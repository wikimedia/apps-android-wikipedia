package org.wikipedia.pageimages;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;

import java.util.List;

public class PageImagesTask extends PageQueryTask<String> {
    private final int thumbSize;
    private final int thumbsCount;

    public PageImagesTask(Api api, Site site, List<PageTitle> titles, int thumbSize) {
        super(api, site, titles);
        this.thumbSize = thumbSize;
        this.thumbsCount = titles.size();
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageimages")
               .param("piprop", "thumbnail")
               .param("pithumbsize", Integer.toString(thumbSize))
               .param("pilimit", Integer.toString(thumbsCount));
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
