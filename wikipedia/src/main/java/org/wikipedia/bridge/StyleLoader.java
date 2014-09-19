package org.wikipedia.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.settings.PrefKeys;

import java.text.ParseException;
import java.util.Date;

/**
* Class that helps with loading a style bundle for different scenarios.
*/
public class StyleLoader {
    public static final String BUNDLE_PAGEVIEW = "styles.css";
    public static final String BUNDLE_PREVIEW = "preview.css";
    public static final String BUNDLE_ABUSEFILTER = "abusefilter.css";
    public static final String BUNDLE_NIGHT_MODE = "night.css";

    private final Context context;
    private final Date assetsUpdated;
    private final SharedPreferences prefs;


    public StyleLoader(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            this.assetsUpdated = Utils.parseISO8601(context.getString(R.string.bundled_styles_updated));
        } catch (ParseException e) {
            // This does not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a currently available bundle of styles of a specific type.
     *
     * Currently it just returns the packaged style bundles. In the future
     * commits it will return either the packaged style bundles or a more
     * recent downloaded bundle (if available).
     *
     * @return
     */
    public StyleBundle getAvailableBundle(String type) {
        if (prefs.contains(PrefKeys.getStylesLastUpdated())) {
            Date downloadUpdated;
            try {
                downloadUpdated = Utils.parseISO8601(prefs.getString(PrefKeys.getStylesLastUpdated(), ""));
            } catch (ParseException e) {
                // This does not happen
                throw new RuntimeException(e);
            }
            if (downloadUpdated.getTime() - assetsUpdated.getTime() > 0) {
                Log.d("Wikipedia", "Using downloaded styles");
                return new DownloadedStyleBundle(type);
            }
        }
        Log.d("Wikipedia", "Using packaged styles");
        return new PackagedStyleBundle(type);
    }
}
