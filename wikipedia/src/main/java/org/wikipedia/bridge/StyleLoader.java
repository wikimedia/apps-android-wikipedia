package org.wikipedia.bridge;

import android.content.*;
import org.wikipedia.*;

/**
* Class that helps with loading a style bundle for different scenarios.
*/
public class StyleLoader {
    public static final String BUNDLE_PAGEVIEW = "styles.css";
    public static final String BUNDLE_PREVIEW = "preview.css";
    public static final String BUNDLE_ABUSEFILTER = "abusefilter.css";

    private final Context context;

    public StyleLoader(Context context) {
        this.context = context;
    }

    /**
     * Returns a currently available bundle of styles of a specific type.
     *
     * Currently it just returns the packaged style bundles. In the future
     * commits it will return either the packaged style bundles or a more
     * recent downloaded bundle (if available).
     *
     * @param site The site to return bundles for.
     * @return
     */
    public StyleBundle getAvailableBundle(String type, Site site) {
        return new PackagedStyleBundle(type);
    }
}
