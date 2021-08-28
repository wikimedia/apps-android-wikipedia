package org.wikipedia.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L

/**
 * To test the Google Play Install Referrer functionality:
 *
 * - Make sure the app is uninstalled.
 * - Launch the Play Store via the usual specially crafted campaign link, such as:
 * https://play.google.com/store/apps/details?id=org.wikipedia&referrer=utm_source%3Dtest_source%26utm_medium%3Dtest_medium%26utm_term%3Dtest-term%26utm_content%3Dtest_content%26utm_campaign%3Dtest_name
 *
 * - ...But do NOT click the "install" button in the Play Store to install the app.
 * - Launch or debug the app in the usual way.
 * - The Install Referrer service should work, and will pass through the correct referrer.
 *
 */
class InstallReferrerListener : InstallReferrerStateListener {

    private var referrerClient: InstallReferrerClient? = null

    private fun queryReferrer(context: Context) {
        try {
            referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient?.startConnection(this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onInstallReferrerSetupFinished(responseCode: Int) {
        when (responseCode) {
            InstallReferrerClient.InstallReferrerResponse.OK -> {
                processInstallReferrer()
                Prefs.setInstallReferrerAttempts(Int.MAX_VALUE)
            }
            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> { }
            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> { }
        }
        WikipediaApp.instance.mainThreadHandler.post {
            referrerClient?.endConnection()
            referrerClient = null
            INSTANCE = null
        }
    }

    override fun onInstallReferrerServiceDisconnected() {
        referrerClient = null
        INSTANCE = null
    }

    /**
     * Receives a referrer string from the Play Store app upon first launch after the app has been
     * installed. If the original link that led the user to the Play Store for installing
     * the app contained a "referrer" query parameter, then the contents of this parameter
     * will be passed to this receiver.
     *
     * The structure for the "referrer" parameter shall be as follows:
     *
     * referrer_url=foo&utm_medium=bar&utm_campaign=baz&utm_source=baz
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
    private fun processInstallReferrer() {
        if (referrerClient == null || !referrerClient!!.isReady) {
            return
        }
        val referrerStr = try {
            referrerClient?.installReferrer?.installReferrer
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        L.d("Received install referrer: $referrerStr")
        if (referrerStr.isNullOrEmpty()) {
            return
        }
        var refUrl: String? = null
        var refUtmMedium: String? = null
        var refUtmCampaign: String? = null
        var refUtmSource: String? = null
        var refChannel: String? = null
        try {
            val arr = referrerStr.split("&".toRegex()).toTypedArray()
            for (str in arr) {
                val item = str.split("=".toRegex()).toTypedArray()
                if (item.size < 2) {
                    continue
                }
                when (item[0]) {
                    InstallReferrerFunnel.PARAM_REFERRER_URL -> refUrl = item[1]
                    InstallReferrerFunnel.PARAM_UTM_MEDIUM -> refUtmMedium = item[1]
                    InstallReferrerFunnel.PARAM_UTM_CAMPAIGN -> refUtmCampaign = item[1]
                    InstallReferrerFunnel.PARAM_UTM_SOURCE -> refUtmSource = item[1]
                    InstallReferrerFunnel.PARAM_CHANNEL -> refChannel = item[1]
                }
            }
        } catch (e: Exception) {
            // Can be thrown by getQueryParameter() if the referrer is malformed.
            // Don't worry about it.
        }
        // log the event only if at least one of the parameters is nonempty
        if (!refUrl.isNullOrEmpty() || !refUtmMedium.isNullOrEmpty() ||
                !refUtmCampaign.isNullOrEmpty() || !refUtmSource.isNullOrEmpty()) {
            val funnel = InstallReferrerFunnel(WikipediaApp.instance)
            funnel.logInstall(refUrl, refUtmMedium, refUtmCampaign, refUtmSource)
        }
        if (!refUrl.isNullOrEmpty() && ShareUtil.canOpenUrlInApp(WikipediaApp.instance, refUrl)) {
            openPageFromUrl(WikipediaApp.instance, refUrl)
        }
        if (!refChannel.isNullOrEmpty()) {
            Prefs.setAppChannel(refChannel)
        }
    }

    private fun openPageFromUrl(context: Context, url: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClass(context, PageActivity::class.java)
        context.startActivity(intent)
    }

    companion object {
        private var INSTANCE: InstallReferrerListener? = null

        @JvmStatic
        fun newInstance(context: Context) {
            val attempts = Prefs.getInstallReferrerAttempts()
            if (attempts > 2) {
                return
            }
            Prefs.setInstallReferrerAttempts(attempts + 1)
            INSTANCE = InstallReferrerListener()
            INSTANCE?.queryReferrer(context)
        }
    }
}
