package org.wikipedia.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;

public final class InstallReceiver extends BroadcastReceiver {
    private static final String INSTALL_ACTION = "com.android.vending.INSTALL_REFERRER";
    private static final String REFERRER_KEY = "referrer";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case INSTALL_ACTION:
                // for play store devices only:
                // * invoke the receiver and open the page:
                //     `adb shell am broadcast -a com.android.vending.INSTALL_REFERRER -n org.wikipedia.dev/org.wikipedia.analytics.InstallReceiver --es "referrer" "referrer_url=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FWombat&campaign_id=foo&install_id=bar"`
                // * invoke the receiver but don't open the app (bad url):
                //     `adb shell am broadcast -a com.android.vending.INSTALL_REFERRER -n org.wikipedia.dev/org.wikipedia.analytics.InstallReceiver --es "referrer" "referrer_url=gibberish&campaign_id=foo&install_id=bar"`
                installReferrer(ctx, intent);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                // `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`
                recordChannelId(ctx);
                break;
            default:
                L.d("action=" + action);
                break;
        }
    }

    /**
     * Receives an Intent from the Play Store app upon first launch after the app has been
     * installed. If the original link that led the user to the Play Store for installing
     * the app contained a "referrer" query parameter, then the contents of this parameter
     * will be passed to this receiver.
     *
     * The structure for the "referrer" parameter shall be as follows:
     *
     *      referrer_url=foo&utm_medium=bar&utm_campaign=baz&utm_source=baz
     *
     * referrer_url: the original url from which the link was clicked.
     * utm_medium: the "medium" from which this install came, e.g. "sitenotice"
     * utm_campaign: name of the campaign from which this install came, e.g. "fundraising2017"
     * utm_source: name of the specific source in the campaign from which this install came, e.g. "popup1"
     *
     * The string containing all of the above parameters is then Urlencoded and passed as the
     * "referrer" parameter in the real URL that leads to the Play Store, which then gets passed
     * down to the app when it's installed.
     *
     * @param ctx Context in which this intent is received.
     * @param intent Intent that contains referrer data from the Play Store.
     */
    private void installReferrer(@NonNull Context ctx, @NonNull Intent intent) {
        String referrerStr = intent.getStringExtra(REFERRER_KEY);
        L.d("Received install referrer: " + referrerStr);
        if (TextUtils.isEmpty(referrerStr)) {
            return;
        }
        if (WikipediaApp.getInstance() == null) {
            L.e("Received install event before WikipediaApp was initialized.");
            return;
        }

        String refUrl = null;
        String refUtmMedium = null;
        String refUtmCampaign = null;
        String refUtmSource = null;
        String refChannel = null;
        try {
            // build a proper dummy URI with the referrer appended to it, so that we can parse it.
            Uri uri = Uri.parse("/?" + referrerStr);
            refUrl = uri.getQueryParameter(InstallReferrerFunnel.PARAM_REFERRER_URL);
            refUtmMedium = uri.getQueryParameter(InstallReferrerFunnel.PARAM_UTM_MEDIUM);
            refUtmCampaign = uri.getQueryParameter(InstallReferrerFunnel.PARAM_UTM_CAMPAIGN);
            refUtmSource = uri.getQueryParameter(InstallReferrerFunnel.PARAM_UTM_SOURCE);
            refChannel = uri.getQueryParameter(InstallReferrerFunnel.PARAM_CHANNEL);
        } catch (UnsupportedOperationException e) {
            // Can be thrown by getQueryParameter() if the referrer is malformed.
            // Don't worry about it.
        }
        // log the event only if at least one of the parameters is nonempty
        if (!TextUtils.isEmpty(refUrl) || !TextUtils.isEmpty(refUtmMedium)
                || !TextUtils.isEmpty(refUtmCampaign) || !TextUtils.isEmpty(refUtmSource)) {
            InstallReferrerFunnel funnel = new InstallReferrerFunnel(WikipediaApp.getInstance());
            funnel.logInstall(refUrl, refUtmMedium, refUtmCampaign, refUtmSource);
        }
        if (!TextUtils.isEmpty(refUrl) && ShareUtil.canOpenUrlInApp(ctx, refUrl)) {
            openPageFromUrl(ctx, refUrl);
        }
        if (!TextUtils.isEmpty(refChannel)) {
            Prefs.setAppChannel(refChannel);
        }
    }

    private void recordChannelId(@NonNull Context ctx) {
        String channel = ReleaseUtil.getChannel(ctx);
        L.v("channel=" + channel);
    }

    private void openPageFromUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, PageActivity.class);
        context.startActivity(intent);
    }
}
