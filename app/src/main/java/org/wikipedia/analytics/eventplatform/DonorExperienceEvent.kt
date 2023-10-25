package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/app_interaction/1.0.0")
class DonorExperienceEvent(
    private val action: String,
    private val active_interface: String,
    private val action_data: String,
    private val primary_language: String,
    private val wiki_id: String,
    private var platform: String
) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "app_donor_experience"

        fun logImpression(activeInterface: String, campaignId: String? = null, wikiId: String = "") {
            submitDonorExperienceEvent("impression", activeInterface, getActionDataString(campaignId), wikiId)
        }

        fun logAction(
            action: String,
            activeInterface: String,
            wikiId: String = "",
            campaignId: String? = null
        ) {
            submitDonorExperienceEvent(
                action,
                activeInterface,
                getActionDataString(campaignId),
                wikiId
            )
        }

        fun getActionDataString(campaignId: String? = null): String {
            return campaignId?.let { "campaign_id: $it, " }.orEmpty()
        }

        private fun submitDonorExperienceEvent(
            action: String,
            activeInterface: String,
            actionData: String,
            wikiId: String
        ) {
            EventPlatformClient.submit(
                DonorExperienceEvent(
                    action,
                    activeInterface,
                    actionData,
                    WikipediaApp.instance.languageState.appLanguageCode,
                    wikiId,
                    "android"
                )
            )
        }
    }
}
