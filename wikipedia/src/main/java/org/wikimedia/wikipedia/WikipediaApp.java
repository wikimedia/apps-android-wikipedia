package org.wikimedia.wikipedia;

import android.app.Application;
import org.mediawiki.api.json.Api;

public class WikipediaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    Api primarySiteAPI;
    public Api getPrimarySiteAPI() {
        if (primarySiteAPI == null) {
            //TODO: Make this configurable
            primarySiteAPI =  new Api("en.wikipedia.org");
        }
        return primarySiteAPI;
    }
}
