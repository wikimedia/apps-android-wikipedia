package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class ImageRecommendationsFunnel : Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun logSubmit(lang: String, pageTitle: String, imageTitle: String, suggestionSource: String,
                  response: Int, reasons: List<Int>, detailsClicked: Boolean, infoClicked: Boolean, scrolled: Boolean,
                  timeUntilClick: Long, timeUntilSubmit: Long, userName: String?, teacherMode: Boolean) {
        log("lang", lang,
                "page_title", pageTitle,
                "image_title", imageTitle,
                "suggestion_source", suggestionSource,
                "response", response,
                "reason", reasons.joinToString(","),
                "info_clicked", infoClicked,
                "details_clicked", detailsClicked,
                "scrolled", scrolled,
                "time_until_click", timeUntilClick,
                "time_until_submit", timeUntilSubmit,
                "user_name", userName,
                "teacher_mode", teacherMode)
    }

    companion object {
        const val RESPONSE_ACCEPT = 0
        const val RESPONSE_REJECT = 1
        const val RESPONSE_NOT_SURE = 2

        private const val SCHEMA_NAME = "MobileWikiAppImageRecommendations"
        private const val REV_ID = 21233672
    }
}
