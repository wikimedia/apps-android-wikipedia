package org.wikimedia.wikipedia;

import android.app.Application;
import com.squareup.otto.Bus;
import org.mediawiki.api.json.Api;

public class WikipediaApp extends Application {
    private Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();
        bus = new Bus();
    }

    public Bus getBus() {
        return bus;
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
