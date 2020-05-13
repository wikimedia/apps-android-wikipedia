package org.wikipedia.analytics;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;

/**
 * To test the Google Play Install Referrer functionality:
 *
 * - Make sure the app is uninstalled.
 * - Launch the Play Store via the usual specially crafted campaign link, such as:
 *   https://play.google.com/store/apps/details?id=org.wikipedia&referrer=utm_source%3Dtest_source%26utm_medium%3Dtest_medium%26utm_term%3Dtest-term%26utm_content%3Dtest_content%26utm_campaign%3Dtest_name
 *
 * - ...But do NOT click the "install" button in the Play Store to install the app.
 * - Launch or debug the app in the usual way.
 * - The Install Referrer service should work, and will pass through the correct referrer.
 *
 */
public class InstallReferrerListener implements InstallReferrerStateListener {
    private static InstallReferrerListener INSTANCE;
    private InstallReferrerClient referrerClient;

    public static void newInstance(@NonNull Context context) {
        int attempts = Prefs.getInstallReferrerAttempts();
        if (attempts > 2) {
            return;
        }
        Prefs.setInstallReferrerAttempts(attempts + 1);
        INSTANCE = new InstallReferrerListener();
        INSTANCE.queryReferrer(context);
    }

    private void queryReferrer(@NonNull Context context) {
        try {
            referrerClient = InstallReferrerClient.newBuilder(context).build();
            referrerClient.startConnection(this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInstallReferrerSetupFinished(int responseCode) {
        switch (responseCode) {
            case InstallReferrerClient.InstallReferrerResponse.OK:
                processInstallReferrer();
                Prefs.setInstallReferrerAttempts(Integer.MAX_VALUE);
                break;
            case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                // API not available on the current Play Store app.
                break;
            case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                // Connection couldn't be established.
                break;
            default:
                break;
        }

        WikipediaApp.getInstance().getMainThreadHandler().post(() -> {
            if (referrerClient != null) {
                referrerClient.endConnection();
            }
            referrerClient = null;
            INSTANCE = null;
        });
    }

    @Override
    public void onInstallReferrerServiceDisconnected() {
        referrerClient = null;
        INSTANCE = null;
    }

    /**
     * Receives a referrer string from the Play Store app upon first launch after the app has been
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
     */
    private void processInstallReferrer() {
        if (referrerClient == null || !referrerClient.isReady()) {
            return;
        }

        String referrerStr;
        try {
            referrerStr = referrerClient.getInstallReferrer().getInstallReferrer();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        L.d("Received install referrer: " + referrerStr);
        if (TextUtils.isEmpty(referrerStr)) {
            return;
        }

        String refUrl = null;
        String refUtmMedium = null;
        String refUtmCampaign = null;
        String refUtmSource = null;
        String refChannel = null;
        try {
            String[] arr = referrerStr.split("&");
            for (String str : arr) {
                String[] item = str.split("=");
                if (item.length < 2) {
                    continue;
                }
                switch (item[0]) {
                    case InstallReferrerFunnel.PARAM_REFERRER_URL:
                        refUrl = item[1];
                        break;
                    case InstallReferrerFunnel.PARAM_UTM_MEDIUM:
                        refUtmMedium = item[1];
                        break;
                    case InstallReferrerFunnel.PARAM_UTM_CAMPAIGN:
                        refUtmCampaign = item[1];
                        break;
                    case InstallReferrerFunnel.PARAM_UTM_SOURCE:
                        refUtmSource = item[1];
                        break;
                    case InstallReferrerFunnel.PARAM_CHANNEL:
                        refChannel = item[1];
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            // Can be thrown by getQueryParameter() if the referrer is malformed.
            // Don't worry about it.
        }
        // log the event only if at least one of the parameters is nonempty
        if (!TextUtils.isEmpty(refUrl) || !TextUtils.isEmpty(refUtmMedium)
                || !TextUtils.isEmpty(refUtmCampaign) || !TextUtils.isEmpty(refUtmSource)) {
            InstallReferrerFunnel funnel = new InstallReferrerFunnel(WikipediaApp.getInstance());
            funnel.logInstall(refUrl, refUtmMedium, refUtmCampaign, refUtmSource);
        }
        if (!TextUtils.isEmpty(refUrl) && ShareUtil.canOpenUrlInApp(WikipediaApp.getInstance(), refUrl)) {
            openPageFromUrl(WikipediaApp.getInstance(), refUrl);
        }
        if (!TextUtils.isEmpty(refChannel)) {
            Prefs.setAppChannel(refChannel);
        }
    }

    private void openPageFromUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, PageActivity.class);
        context.startActivity(intent);
    }
}
