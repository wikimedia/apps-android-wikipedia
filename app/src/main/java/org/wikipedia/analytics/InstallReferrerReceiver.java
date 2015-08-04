package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.wikipedia.util.log.L;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public final class InstallReferrerReceiver extends BroadcastReceiver {
    private static final String INSTALL_ACTION = "com.android.vending.INSTALL_REFERRER";
    private static final String REFERRER_KEY = "referrer";

    /**
     * Receives an Intent from the Play Store app upon first launch after the app has been
     * installed. If the original link that led the user to the Play Store for installing
     * the app contained a "referrer" query parameter, then the contents of this parameter
     * will be passed to this receiver.
     *
     * For a breakdown of the structure of the referrer string, please refer to this task:
     * https://phabricator.wikimedia.org/T103460
     *
     * @param ctx Context in which this intent is received.
     * @param intent Intent that contains referrer data from the Play Store.
     */
    public void onReceive(Context ctx, Intent intent) {
        String referrerStr = intent.getStringExtra(REFERRER_KEY);
        L.d("Received install referrer: " + referrerStr);
        if (!INSTALL_ACTION.equals(intent.getAction()) || TextUtils.isEmpty(referrerStr)) {
            return;
        }

        // build a proper dummy URI with the referrer appended to it, so that we can parse it.
        Uri uri = Uri.parse("/?" + referrerStr);

        // initialize the funnel with a dummy Site, since this is happening outside of
        // any kind of browsing or site interactions.
        InstallReferrerFunnel funnel = new InstallReferrerFunnel(WikipediaApp.getInstance());
        // and send the event!
        funnel.logInstall(uri.getQueryParameter(InstallReferrerFunnel.PARAM_REFERRER_URL),
                          uri.getQueryParameter(InstallReferrerFunnel.PARAM_CAMPAIGN_ID),
                          uri.getQueryParameter(InstallReferrerFunnel.PARAM_CAMPAIGN_INSTALL_ID));
    }
}
