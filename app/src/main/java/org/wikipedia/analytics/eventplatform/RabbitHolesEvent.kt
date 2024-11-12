package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp

object RabbitHolesEvent {
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
                "rabbit_holes"
            )
        )
    }
}
