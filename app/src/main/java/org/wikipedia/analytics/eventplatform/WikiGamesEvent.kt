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
        isArchive: Boolean? = null,
        langCode: String? = null,
        cardType: String? = null,
        position: Int? = null,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        EventPlatformClient.submit(
            AppInteractionEvent(
                action,
                activeInterface,
                JsonUtil.encodeToString(ActionData(
                    slide = slideName,
                    archive = isArchive,
                    feedbackText = feedbackText,
                    feedbackSelect = feedbackSelect,
                    langCode = langCode,
                    cardType = cardType,
                    position = position
                )).orEmpty(),
                WikipediaApp.instance.languageState.appLanguageCode,
                wikiId,
                "app_game_interaction"
            )
        )
    }

    @Serializable
    class ActionData(
        val slide: String? = null,
        val archive: Boolean? = null,
        @SerialName("feedback_select") val feedbackSelect: String? = null,
        @SerialName("feedback_text") val feedbackText: String? = null,
        @SerialName("lang_code") val langCode: String? = null,
        @SerialName("card_type") val cardType: String? = null,
        @SerialName("position") val position: Int? = null,
    )
}
