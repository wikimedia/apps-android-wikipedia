package org.wikipedia.networking;

import org.mediawiki.api.json.Api;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import retrofit.RequestInterceptor;

import java.util.HashMap;

public class MccMncStateHandler {
    private boolean mccMncSent = false;

    /**
     * Enriches request to have a header with the MCC-MNC (mobile operator code) if
     * cellular data connection is the active one and it hasn't already been sent
     * and the user isn't currently opted out of event logging.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     *
     * @param app app instance
     * @param site currently active site
     * @param customHeaders HashMap of custom headers
     * @return API enriched with MCC-MNC headers, or null if headers aren't necessary
     */
    public Api makeApiWithMccMncHeaderEnrichment(WikipediaApp app, Site site,
                                                 HashMap<String, String> customHeaders) {
        if (shouldSendHeader(app, site.getDomain())) {
            String mccMnc = NetworkUtil.getMccMnc(app);
            if (mccMnc != null) {
                customHeaders.put("X-MCCMNC", mccMnc);
                this.mccMncSent = true;
                return new Api(site.getDomain(), customHeaders);
            }
        }
        return null;
    }

    /**
     * Enriches request to have a header with the MCC-MNC (mobile operator code) if
     * cellular data connection is the active one and it hasn't already been sent
     * and the user isn't currently opted out of event logging.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     *
     * This is the equivalent of #makeApiWithMccMncHeaderEnrichment for Retrofit
     *
     * @param app app instance
     * @param domain currently active API domain
     * @param request Retrofit request
     */
    public void injectMccMncHeader(WikipediaApp app, String domain,
                                   RequestInterceptor.RequestFacade request) {
        if (shouldSendHeader(app, domain)) {
            String mccMnc = NetworkUtil.getMccMnc(app);
            if (mccMnc != null) {
                request.addHeader("X-MCCMNC", mccMnc);
                this.mccMncSent = true;
            }
        }
    }

    private boolean shouldSendHeader(WikipediaApp app, String domain) {
        // Skip if it was already sent or user opted out of logging
        // or the API server isn't a mobile Wikipedia.
        return !this.mccMncSent
                && app.isEventLoggingEnabled()
                && domain.contains(".m.wikipedia.org");
    }
}
