package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

object RecommendedReadingListEvent {

    fun submit(
        action: String,
        activeInterface: String,
        groupName: String,
        wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode
    ) {
        val actionData = ActionData(
            rrlGroup = groupName
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
        @SerialName("rrl_group")
        val rrlGroup: String
    )
}
