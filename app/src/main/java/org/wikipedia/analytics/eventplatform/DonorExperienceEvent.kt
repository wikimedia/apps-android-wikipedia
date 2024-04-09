package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp

class DonorExperienceEvent {

    companion object {
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
}
