package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.util.ShareUtil;
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

        String refUrl = null;
        String refCampaignId = null;
        String refCampaignInstallId = null;
        try {
            // build a proper dummy URI with the referrer appended to it, so that we can parse it.
            Uri uri = Uri.parse("/?" + referrerStr);
            refUrl = uri.getQueryParameter(InstallReferrerFunnel.PARAM_REFERRER_URL);
            refCampaignId = uri.getQueryParameter(InstallReferrerFunnel.PARAM_CAMPAIGN_ID);
            refCampaignInstallId = uri.getQueryParameter(InstallReferrerFunnel.PARAM_CAMPAIGN_INSTALL_ID);
        } catch (UnsupportedOperationException e) {
            // Can be thrown by getQueryParameter() if the referrer is malformed.
            // Don't worry about it.
        }
        // log the event only if at least one of the parameters is nonempty
        if (!TextUtils.isEmpty(refUrl) || !TextUtils.isEmpty(refCampaignId)
                || !TextUtils.isEmpty(refCampaignInstallId)) {
            InstallReferrerFunnel funnel = new InstallReferrerFunnel(WikipediaApp.getInstance());
            funnel.logInstall(refUrl, refCampaignId, refCampaignInstallId);
        }
        if (!TextUtils.isEmpty(refUrl) && ShareUtil.canOpenUrlInApp(ctx, refUrl)) {
            openPageFromUrl(ctx, refUrl);
        }
    }

    private void openPageFromUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, PageActivity.class);
        context.startActivity(intent);
    }
}
