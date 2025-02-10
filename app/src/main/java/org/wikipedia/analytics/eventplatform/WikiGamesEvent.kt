package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

object WikiGamesEvent {
    fun submit(
        action: String,
        activeInterface: String,
        slideName: String? = null,
        feedbackSelect: String? = null,
        feedbackText: String? = null,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        EventPlatformClient.submit(
            AppInteractionEvent(
                action,
                activeInterface,
                JsonUtil.encodeToString(ActionData(
                    slide = slideName,
                    feedbackText = feedbackText,
                    feedbackSelect = feedbackSelect
                )).orEmpty(),
                WikipediaApp.instance.languageState.appLanguageCode,
                wikiId,
                "app_games"
            )
        )
    }

    @Serializable
    class ActionData(
        val slide: String? = null,
        @SerialName("feedback_select") val feedbackSelect: String? = null,
        @SerialName("feedback_text") val feedbackText: String? = null,
    )
}
