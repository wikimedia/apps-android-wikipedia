package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.donate.CampaignCollection
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs

open class DonorExperienceEvent {

    companion object {

        fun logAction(
            action: String,
            activeInterface: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode,
            campaignId: String? = null
        ) {
            submit(
                action,
                activeInterface,
                campaignId?.let { "campaign_id: ${CampaignCollection.getFormattedCampaignId(it)}, " }.orEmpty() + "banner_opt_in: ${Prefs.donationBannerOptIn}",
                wikiId
            )
        }

        fun logDonationReminderAction(
            action: String,
            activeInterface: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode,
            defaultMilestone: Boolean? = null,
            campaignId: String? = null,
            articleFrequency: Int? = null,
            donateAmount: Float? = null,
            settingSelect: Boolean? = null
        ) {
            val actionData = DonationRemindersActionData(
                defaultMilestone = defaultMilestone,
                campaignId = campaignId,
                articleFrequency = articleFrequency,
                donateAmount = donateAmount,
                settingSelect = settingSelect
            )
            submit(
                action,
                activeInterface,
                JsonUtil.encodeToString(actionData).orEmpty(),
                wikiId
            )
        }

        fun submit(
            action: String,
            activeInterface: String,
            actionData: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
        ) {
            println("orange --> $activeInterface $action $actionData")
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action,
                    activeInterface,
                    actionData,
                    WikipediaApp.instance.languageState.appLanguageCode,
                    wikiId,
                    "app_donor_experience"
                )
            )
        }
    }

    @Serializable
    class DonationRemindersActionData(
        @SerialName("milestone_default") val defaultMilestone: Boolean? = null,
        @SerialName("campaign_id") val campaignId: String? = null,
        @SerialName("read_freq") val articleFrequency: Int? = null,
        @SerialName("donate_amount") val donateAmount: Float? = null,
        @SerialName("setting_select") val settingSelect: Boolean? = null
    )
}
