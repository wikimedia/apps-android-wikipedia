package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class InstallReferrerFunnel internal constructor(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    fun logInstall(referrerUrl: String?, utfMedium: String?,
                   utfCampaign: String?, utfSource: String?) {
        log(
                PARAM_REFERRER_URL, referrerUrl,
                PARAM_UTM_MEDIUM, utfMedium,
                PARAM_UTM_CAMPAIGN, utfCampaign,
                PARAM_UTM_SOURCE, utfSource
        )
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppInstallReferrer"
        private const val REV_ID = 18115554

        // For an explanation of these parameters, refer to the schema documentation:
        // https://meta.wikimedia.org/wiki/Schema:MobileWikiAppInstallReferrer
        const val PARAM_REFERRER_URL = "referrer_url"
        const val PARAM_UTM_MEDIUM = "utm_medium"
        const val PARAM_UTM_CAMPAIGN = "utm_campaign"
        const val PARAM_UTM_SOURCE = "utm_source"
        const val PARAM_CHANNEL = "channel"
    }
}
