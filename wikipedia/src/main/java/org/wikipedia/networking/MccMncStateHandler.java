package org.wikipedia.networking;

import android.content.Context;
import android.preference.PreferenceManager;
import org.mediawiki.api.json.Api;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.HashMap;

public class MccMncStateHandler {
    private boolean mccMncSent = false;
    private WikipediaApp app;

    /**
     * Enriches request to have a header with the MCC-MNC (mobile operator code) if
     * cellular data connection is the active one and it hasn't already been sent
     * and the user isn't currently opted out of event logging.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     * @param ctx Applicaton context
     * @param site Currently active site
     * @param ua The User-Agent with which to augment the request, if applicable (usually yes)
     * @return
     */
    public Api makeApiWithMccMncHeaderEnrichment(Context ctx, Site site, String ua) {
        if (this.app == null) {
            this.app = (WikipediaApp)ctx;
        }
        // Forget about it if it was already sent or user opted out of logging or the API server isn't a mobile Wikipedia.
        if (this.mccMncSent ||
                !PreferenceManager.getDefaultSharedPreferences(this.app).getBoolean(this.app.PREFERENCE_EVENTLOGGING_ENABLED, true) ||
                !(site.getApiDomain().contains(".m.wikipedia.org"))) {
            return null;
        }
        String mccMnc = Utils.getMccMnc(ctx);
        if (mccMnc != null) {
            HashMap<String,String> customHeaders = new HashMap<String,String>();
            customHeaders.put("X-MCCMNC", mccMnc);
            this.mccMncSent = true;
            return new Api(site.getApiDomain(), ua, customHeaders);
        }
        return null;
    }
}
