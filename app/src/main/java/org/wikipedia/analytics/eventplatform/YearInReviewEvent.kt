package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

open class YearInReviewEvent {

    companion object {
        fun submit(
            action: String,
            activeInterface: String = "wiki_yir",
            slide: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
        ) {
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action = action,
                    active_interface = activeInterface,
                    action_data = JsonUtil.encodeToString(ActionData(
                        slide = slide
                    )).orEmpty(),
                    primary_language = WikipediaApp.instance.languageState.appLanguageCode,
                    wiki_id = wikiId,
                    streamName = "app_donor_experience"
                )
            )
        }
    }

    @Serializable
    class ActionData(
        val slide: String? = null,
        @SerialName("campaign_id") val campaignId: String? = null,
        @SerialName("feedback_select") val feedbackSelect: Int? = null,
        @SerialName("feedback_text") val feedbackText: String? = null
    )
}
