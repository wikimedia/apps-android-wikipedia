package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.activitytab.ActivityTabABTest
import org.wikipedia.json.JsonUtil

object ActivityTabEvent {
    fun submit(
        activeInterface: String,
        action: String,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        EventPlatformClient.submit(
            AppInteractionEvent(
                action,
                activeInterface,
                action_data = JsonUtil.encodeToString(ActionData(
                    group = ActivityTabABTest().getGroupName()
                )).orEmpty(),
                WikipediaApp.instance.languageState.appLanguageCode,
                wikiId,
                streamName = "app_activity_tab"
            )
        )
    }

    @Serializable
    class ActionData(
        val group: String? = null,
        val state: String? = null,
        val reading: String? = null,
        val impact: String? = null,
        val games: String? = null,
        val donations: String? = null,
        val timeline: String? = null,
        val all: String? = null,
        @SerialName("edit_count") val editCount: String? = null,
        @SerialName("feedback_select") val feedbackSelect: Int? = null,
        @SerialName("feedback_text") val feedbackText: String? = null
    )
}
