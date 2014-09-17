package org.wikipedia.beta.networking;

import android.content.Context;
import android.preference.PreferenceManager;
import org.mediawiki.api.json.Api;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.settings.PrefKeys;

import java.util.HashMap;

public class MccMncStateHandler {
    private boolean mccMncSent = false;
    private WikipediaApp app;

    /**
     * Enriches request to have a header with the MCC-MNC (mobile operator code) if
     * cellular data connection is the active one and it hasn't already been sent
     * and the user isn't currently opted out of event logging.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     * @param ctx Application context
     * @param site Currently active site
     * @param customHeaders Hashmap of custom headers
     * @return
     */
    public Api makeApiWithMccMncHeaderEnrichment(Context ctx, Site site, HashMap<String, String> customHeaders) {
        if (this.app == null) {
            this.app = (WikipediaApp)ctx;
        }
        // Forget about it if it was already sent or user opted out of logging or the API server isn't a mobile Wikipedia.
        if (this.mccMncSent
            || !PreferenceManager.getDefaultSharedPreferences(this.app).getBoolean(PrefKeys.getEventLoggingEnabled(), true)
            || !(site.getApiDomain().contains(".m.wikipedia.org"))) {
            return null;
        }
        String mccMnc = Utils.getMccMnc(ctx);
        if (mccMnc != null) {
            customHeaders.put("X-MCCMNC", mccMnc);
            this.mccMncSent = true;
            return new Api(site.getApiDomain(), customHeaders);
        }
        return null;
    }
}
