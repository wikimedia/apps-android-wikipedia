package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp

class DonorExperienceEvent {

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
                campaignId?.let { "campaign_id: $it, " }.orEmpty(),
                wikiId
            )
        }

        fun submit(
            action: String,
            activeInterface: String,
            actionData: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
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
