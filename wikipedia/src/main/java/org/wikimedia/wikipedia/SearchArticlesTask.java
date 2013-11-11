package org.wikimedia.wikipedia;

import android.content.Context;
import org.json.JSONArray;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

import java.util.ArrayList;
import java.util.List;

public class SearchArticlesTask extends SaneAsyncTask<List<PageTitle>>{
    private Site site;
    private String prefix;
    private WikipediaApp app;

    private ApiResult result;

    public SearchArticlesTask(Context context, Site site, String prefix) {
        super(ExecutorService.getSingleton().getExecutor(PageFetchTask.class));
        this.app = (WikipediaApp)context.getApplicationContext();
        this.site = site;
        this.prefix = prefix;
    }

    @Override
    public List<PageTitle> performTask() throws Throwable {
        Api api = app.getAPIForSite(site);
        result = api.action("opensearch").param("search", prefix).get();
        JSONArray searchResults = result.asArray().optJSONArray(1);

        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        for (int i = 0; i < searchResults.length(); i++) {
            pageTitles.add(new PageTitle(null, searchResults.optString(i), site));
        }

        return pageTitles;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (result != null) {
            result.cancel();
        }
    }
}
