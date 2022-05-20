package org.wikipedia.analytics.eventplatform

import android.util.Log
import android.view.View
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumb_log_event/1.0.0")
class BreadCrumbLogEvent(private val screen_name: String,
                         private val action: String,
                         private val primary_language_code: String) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.breadcrumb_log_event"

        fun log(screenName: String, view: View?) {
            // EventPlatformClient.submit(BreadCrumbLogEvent(screenName, BreadCrumbViewUtil.getLogNameForView(view), WikipediaApp.getInstance().language().appLanguageCode))
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + screenName + "|\t|ACTION|\t|" + BreadCrumbViewUtil.getLogNameForView(view) + "|")
        }
    }
}
