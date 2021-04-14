package org.wikipedia.analytics.eventplatform

class ImageRecommendationsEvent(private val lang: String, private val page_title: String, private val image_title: String,
                                private val suggestion_source: String, private val response_type: Int, private val reasons: List<Int>,
                                private val info_clicked: Boolean, private val details_clicked: Boolean, scrolled: Boolean,
                                private val time_until_click_ms: Long, private val time_until_submit_ms: Long, private val user_text: String?,
                                private val teacher_mode_enabled: Boolean) : Event(SCHEMA_NAME, STREAM_NAME) {
    companion object {
        private const val SCHEMA_NAME = "/analytics/mobile_apps/android_image_recommendation_interaction/1.0.0"
        private const val STREAM_NAME = "android.image_recommendation_interaction"

        fun logImageRecommendationInteraction(lang: String, pageTitle: String, imageTitle: String, suggestionSource: String,
                                              response: Int, reasons: List<Int>, detailsClicked: Boolean, infoClicked: Boolean, scrolled: Boolean,
                                              timeUntilClick: Long, timeUntilSubmit: Long, userName: String?, teacherMode: Boolean) {
            EventPlatformClient.submit(ImageRecommendationsEvent(lang, pageTitle, imageTitle, suggestionSource,
                    response, reasons, infoClicked, detailsClicked, scrolled, timeUntilClick, timeUntilSubmit,
                    userName, teacherMode))
        }
    }
}
