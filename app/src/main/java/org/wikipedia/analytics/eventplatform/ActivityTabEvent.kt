package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

// TODO: ACTIVITY_TAB_INSTRUMENTATION send events from onboarding screen
object ActivityTabEvent {
    fun submit(
        activeInterface: String,
        action: String,
        group: String? = null,
        editCount: Int? = null,
        state: String? = null,
        timeSpent: String? = null,
        readingInsight: String? = null,
        editingInsight: String? = null,
        impact: String? = null,
        games: String? = null,
        donations: String? = null,
        timeline: String? = null,
        all: String? = null,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        EventPlatformClient.submit(
            AppInteractionEvent(
                action = action,
                active_interface = activeInterface,
                action_data = JsonUtil.encodeToString(ActionData(
                    group = group,
                    editCount = editCount,
                    state = state,
                    timeSpent = timeSpent,
                    readingInsight = readingInsight,
                    editingInsight = editingInsight,
                    impact = impact,
                    games = games,
                    donations = donations,
                    timeline = timeline,
                    all = all
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
        val impact: String? = null,
        val games: String? = null,
        val donations: String? = null,
        val timeline: String? = null,
        val all: String? = null,
        @SerialName("time_spent") val timeSpent: String? = null,
        @SerialName("reading_insight") val readingInsight: String? = null,
        @SerialName("editing_insight") val editingInsight: String? = null,
        @SerialName("edit_count") val editCount: Int? = null,
        @SerialName("feedback_select") val feedbackSelect: Int? = null,
        @SerialName("feedback_text") val feedbackText: String? = null
    )
}
