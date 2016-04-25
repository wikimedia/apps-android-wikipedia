package org.wikipedia.readinglist;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.Site;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;

import java.util.List;

public class ReadingListPageInfoTask extends PageQueryTask<Void> {
    private List<PageTitle> titles;
    private final int thumbSize;
    private final int pageCount;

    public ReadingListPageInfoTask(Api api, Site site, List<PageTitle> titles, int thumbSize) {
        super(api, site, titles);
        this.titles = titles;
        this.thumbSize = thumbSize;
        this.pageCount = titles.size();
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageimages|pageterms")
                .param("piprop", "thumbnail")
                .param("pithumbsize", Integer.toString(thumbSize))
                .param("pilimit", Integer.toString(pageCount));
    }

    @Override
    public Void processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        for (PageTitle title : titles) {
            if (title.getDisplayText().equals(pageTitle.getDisplayText())) {
                JSONObject thumbnail = pageData.optJSONObject("thumbnail");
                if (thumbnail != null) {
                    title.setThumbUrl(thumbnail.optString("source"));
                }
                JSONObject terms = pageData.optJSONObject("terms");
                if (terms != null) {
                    JSONArray description = terms.optJSONArray("description");
                    if (description != null) {
                        title.setDescription(description.optString(0));
                    }
                }
            }
        }
        return null;
    }
}