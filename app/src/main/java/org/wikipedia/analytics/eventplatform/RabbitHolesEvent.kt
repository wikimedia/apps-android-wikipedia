package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.metricsplatform.RabbitHolesAnalyticsHelper
import org.wikipedia.json.JsonUtil

object RabbitHolesEvent {
    fun submit(
        action: String,
        activeInterface: String,
        source: String? = null,
        feedbackSelect: String? = null,
        feedbackText: String? = null,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        EventPlatformClient.submit(
            AppInteractionEvent(
                action,
                activeInterface,
                JsonUtil.encodeToString(ActionData(
                    groupAssigned = RabbitHolesAnalyticsHelper.abcTest.getGroupName(),
                    source = source,
                    feedbackText = feedbackText,
                    feedbackSelect = feedbackSelect
                )).orEmpty(),
                WikipediaApp.instance.languageState.appLanguageCode,
                wikiId,
                "app_rabbit_holes"
            )
        )
    }

    @Serializable
    class ActionData(
        @SerialName("group_assigned") val groupAssigned: String? = null,
        @SerialName("source") val source: String? = null,
        @SerialName("feedback_select") val feedbackSelect: String? = null,
        @SerialName("feedback_text") val feedbackText: String? = null,
    )
}
