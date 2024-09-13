package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_install_referrer_event/1.1.0")
class InstallReferrerEvent(@SerialName("referrer_url") private val referrerUrl: String,
                           @SerialName("campaign_id") private val campaignId: String,
                           @SerialName("utm_medium") private val utfMedium: String,
                           @SerialName("utm_campaign") private val utfCampaign: String,
                           @SerialName("utm_source") private val utfSource: String) :
    MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.install_referrer_event"
        const val PARAM_REFERRER_URL = "referrer_url"
        const val PARAM_UTM_MEDIUM = "utm_medium"
        const val PARAM_UTM_CAMPAIGN = "utm_campaign"
        const val PARAM_UTM_SOURCE = "utm_source"
        const val PARAM_CHANNEL = "channel"
        fun logInstall(referrerUrl: String?, utfMedium: String?, utfCampaign: String?, utfSource: String?) {
            EventPlatformClient.submit(InstallReferrerEvent(referrerUrl.orEmpty(), "android", utfMedium.toString(), utfCampaign.toString(), utfSource.toString()))
        }
    }
}
