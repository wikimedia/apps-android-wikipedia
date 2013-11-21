package org.wikimedia.wikipedia;

import android.*;
import android.R;
import android.app.Application;
import com.squareup.otto.Bus;
import org.mediawiki.api.json.Api;

import java.util.HashMap;

public class WikipediaApp extends Application {
    private Bus bus;

    public static long SHORT_ANIMATION_DURATION;
    public static long MEDIUM_ANIMATION_DURATION;

    public static float SCREEN_DENSITY;

    @Override
    public void onCreate() {
        super.onCreate();
        bus = new Bus();

        SHORT_ANIMATION_DURATION = getResources().getInteger(R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = getResources().getInteger(R.integer.config_mediumAnimTime);
        SCREEN_DENSITY = getResources().getDisplayMetrics().density;
    }

    public Bus getBus() {
        return bus;
    }

    public Api getPrimarySiteAPI() {
        return getAPIForSite(getPrimarySite());
    }

    private HashMap<String, Api> apis = new HashMap<String, Api>();
    public Api getAPIForSite(Site site) {
        if (!apis.containsKey(site.getDomain()))  {
            apis.put(site.getDomain(), new Api(site.getDomain()));
        }
        return apis.get(site.getDomain());
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
