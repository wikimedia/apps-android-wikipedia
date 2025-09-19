package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

object RecommendedReadingListEvent {

    fun submit(
        action: String,
        activeInterface: String,
        optionsShown: String? = null,
        countSelected: Int? = null,
        countSaved: Int? = null,
        currentSetting: String? = null,
        selected: String? = null,
        source: String? = null,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        val actionData = ActionData(
            optionsShown = optionsShown,
            countSelected = countSelected,
            countSaved = countSaved,
            currentSetting = currentSetting,
            selected = selected,
            source = source
        )

        EventPlatformClient.submit(
            AppInteractionEvent(
                action,
                activeInterface,
                action_data = JsonUtil.encodeToString(actionData).orEmpty(),
                primary_language = WikipediaApp.instance.languageState.appLanguageCode,
                wiki_id = wikiId,
                streamName = "app_rabbit_holes"
            )
        )
    }

    @Serializable
    class ActionData(
        @SerialName("options_shown") val optionsShown: String? = null,
        @SerialName("count_selected") val countSelected: Int? = null,
        @SerialName("count_saved") val countSaved: Int? = null,
        @SerialName("current_setting") val currentSetting: String? = null,
        val selected: String? = null,
        val source: String? = null
    )
}
