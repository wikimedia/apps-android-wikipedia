package org.wikimedia.wikipedia;

import android.content.Context;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

abstract public class ApiTask<T> extends SaneAsyncTask<T> {
    private Site site;
    private WikipediaApp app;

    private ApiResult result;

    public ApiTask(Context context, Site site) {
        super(ExecutorService.getSingleton().getExecutor(PageFetchTask.class, 2));
        this.site = site;
        this.app = (WikipediaApp)context.getApplicationContext();
    }

    @Override
    public T performTask() throws Throwable {
        Api api = app.getAPIForSite(site);
        result = buildRequest(api);
        return processResult(result);
    }

    // @fixme ApiResult.cancel doesn't actually cancel, instead causes app to crash if run on main thread
    // uncomment this once fixed in java-mwapi
    /*
    @Override
    public void cancel() {
        super.cancel();
        if (result != null) {
            result.cancel();
        }
    }
    */

    public Site getSite() {
        return site;
    }

    abstract public ApiResult buildRequest(Api api);
    abstract public T processResult(ApiResult result) throws Throwable;

}
