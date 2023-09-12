package org.wikipedia.analytics.metricsplatform

class InstallReferrerEvent(
    private val referrerUrl: String,
    private val campaignId: String,
    private val utfMedium: String,
    private val utfCampaign: String,
    private val utfSource: String
) {

    companion object : MetricsEvent() {
        const val PARAM_REFERRER_URL = "referrer_url"
        const val PARAM_UTM_MEDIUM = "utm_medium"
        const val PARAM_UTM_CAMPAIGN = "utm_campaign"
        const val PARAM_UTM_SOURCE = "utm_source"
        const val PARAM_CHANNEL = "channel"

        fun logInstall(referrerUrl: String?, utfMedium: String?, utfCampaign: String?, utfSource: String?) {
            submitEvent(
                "install_referrer_event",
                mapOf(
                    "referrer_url" to referrerUrl.orEmpty(),
                    "utm_medium" to utfMedium.toString(),
                    "utm_campaign" to utfCampaign.toString(),
                    "utm_source" to utfSource.toString(),
                    "campaign_id" to "android"
                )
            )
        }
    }
}
