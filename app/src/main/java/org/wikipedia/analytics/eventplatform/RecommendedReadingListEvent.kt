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
                JsonUtil.encodeToString(actionData).orEmpty(),
                WikipediaApp.instance.languageState.appLanguageCode,
                wikiId
            )
        )
    }

    @Serializable
    class ActionData(
        @SerialName("rrl_group")
        val rrlGroup: String
    )
}
