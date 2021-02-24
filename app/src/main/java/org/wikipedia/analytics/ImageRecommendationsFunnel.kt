package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil.isLoggedIn

class ImageRecommendationsFunnel :
        Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "anon", !isLoggedIn)
        return super.preprocessData(eventData)
    }

    override fun preprocessSessionToken(eventData: JSONObject) { }

    fun logSubmit(lang: String, pageTitle: String, imageTitle: String, response: Int, reasons: List<Int>,
                  detailsClicked: Boolean, infoClicked: Boolean, scrolled: Boolean,
                  timeUntilClick: Int, timeUntilSubmit: Int, userName: String?, teacherMode: Boolean) {
        log("lang", lang,
                "pageTitle", pageTitle,
                "imageTitle", imageTitle,
                "response", response,
                "reason", reasons.joinToString(","),
                "infoClicked", infoClicked,
                "detailsClicked", detailsClicked,
                "scrolled", scrolled,
                "timeUntilClick", timeUntilClick,
                "timeUntilSubmit", timeUntilSubmit,
                "userName", userName,
                "teacherMode", teacherMode)
    }

    companion object {
        const val RESPONSE_ACCEPT = 0
        const val RESPONSE_REJECT = 1
        const val RESPONSE_NOT_SURE = 2

        private const val SCHEMA_NAME = "MobileWikiAppImageRecommendations"
        private const val REV_ID = 21020341
    }
}
