package org.wikipedia.savedpages;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.page.fetch.OldSectionsFetchTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class RefreshSavedPageTask extends OldSectionsFetchTask {
    private final PageTitle title;
    private final WikipediaApp app;

    public RefreshSavedPageTask(WikipediaApp app, PageTitle title) {
        super(app, title, "all");
        this.title = title;
        this.app = app;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder =  super.buildRequest(api);
        builder.param("prop", builder.getParams().get("prop") + "|" + Page.API_REQUEST_PROPS);
        return builder;
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        JSONObject mobileView = result.asObject().optJSONObject("mobileview");
        if (mobileView != null) {
            PageProperties pageProperties = new PageProperties(mobileView);
            List<Section> sections = super.processResult(result);
            final Page page = new Page(title, (ArrayList<Section>) sections, pageProperties);
            final CountDownLatch savePagesLatch = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    new SavePageTask(app, page.getTitle(), page) {
                        @Override
                        public void onFinish(Boolean result) {
                            savePagesLatch.countDown();
                        }
                    }.execute();
                }
            });
            savePagesLatch.await();
            return sections;
        } else {
            return super.processResult(result);
        }
    }
}
