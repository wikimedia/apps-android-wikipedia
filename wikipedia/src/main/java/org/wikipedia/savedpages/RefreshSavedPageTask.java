package org.wikipedia.savedpages;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.Section;
import org.wikipedia.page.SectionsFetchTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class RefreshSavedPageTask extends SectionsFetchTask {
    private final SavedPage savedPage;
    private final WikipediaApp app;

    public RefreshSavedPageTask(Context context, SavedPage savedPage) {
        super(context, savedPage.getTitle(), "all");
        this.savedPage = savedPage;
        this.app = WikipediaApp.getInstance();
    }


    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder =  super.buildRequest(api);
        builder.param("prop", builder.getParams().get("prop") + "|" + Page.API_REQUEST_PROPS);
        builder.param("appInstallID", app.getAppInstallID());
        return builder;
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        JSONObject mobileView = result.asObject().optJSONObject("mobileview");
        if (mobileView != null) {
            PageProperties pageProperties = new PageProperties(mobileView);
            List<Section> sections = super.processResult(result);
            final Page page = new Page(savedPage.getTitle(), (ArrayList<Section>) sections, pageProperties);
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
