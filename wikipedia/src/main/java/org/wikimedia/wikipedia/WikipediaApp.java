package org.wikimedia.wikipedia;

import android.*;
import android.R;
import android.app.Application;
import com.squareup.otto.Bus;
import org.mediawiki.api.json.Api;

public class WikipediaApp extends Application {
    private Bus bus;

    public static long SHORT_ANIMATION_DURATION;
    public static long MEDIUM_ANIMATION_DURATION;

    @Override
    public void onCreate() {
        super.onCreate();
        bus = new Bus();

        SHORT_ANIMATION_DURATION = getResources().getInteger(R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = getResources().getInteger(R.integer.config_mediumAnimTime);

    }

    public Bus getBus() {
        return bus;
    }

    Api primarySiteAPI;
    public Api getPrimarySiteAPI() {
        if (primarySiteAPI == null) {
            primarySiteAPI =  new Api(getPrimarySite().getDomain());
        }
        return primarySiteAPI;
    }

    private Site primarySite;
    public Site getPrimarySite() {
        if (primarySite == null) {
            // FIXME: Actually read from SharedPreferences or something
            primarySite = new Site("en.wikipedia.org");
        }
        return primarySite;
    }
}
