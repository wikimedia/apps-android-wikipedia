package org.wikimedia.wikipedia.pageimages;

import android.util.Log;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikimedia.wikipedia.PageQueryTask;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.Site;
import org.wikimedia.wikipedia.concurrency.ExecutorService;

import java.util.List;
import java.util.concurrent.Executor;

public class PageImagesTask extends PageQueryTask<String> {
    private final int thumbSize;
    private final int thumbsCount;

    protected PageImagesTask(Executor executor, Api api, Site site, List<PageTitle> titles, int thumbSize) {
        super(executor, api, site, titles);
        this.thumbSize = thumbSize;
        this.thumbsCount = titles.size();
    }

    public PageImagesTask(Api api, Site site, List<PageTitle> titles, int thumbSize) {
        super(ExecutorService.getSingleton().getExecutor(PageImagesTask.class, 2), api, site, titles);
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
        Log.d("Wikipedia", "thumb info: " + pageData.toString());
        JSONObject thumbnail = pageData.optJSONObject("thumbnail");
        if (thumbnail == null) {
            return null;
        } else {
            return thumbnail.getString("source");
        }
    }
}
