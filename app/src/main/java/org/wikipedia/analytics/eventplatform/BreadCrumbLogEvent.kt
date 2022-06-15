package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.content.Context
import android.view.View
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.log.L

@Suppress("unused", "CanBeParameter")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumbs_event/1.0.0")
class BreadCrumbLogEvent(private val screen_name: String,
                         private val action: String,
                         private val app_primary_language_code: String) : MobileAppsEvent(STREAM_NAME) {

    init {
        L.d(">>> $screen_name.$action")
    }

    companion object {
        private const val STREAM_NAME = "android.breadcrumbs_event"

        fun logClick(activity: Activity, view: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), activity.getString(R.string.breadcrumb_view_click, viewReadableName), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logLongClick(activity: Activity, view: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), activity.getString(R.string.breadcrumb_view_long_click, viewReadableName), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logSwipe(activity: Activity) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), activity.getString(R.string.breadcrumb_screen_swiped_to), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logScreenShown(activity: Activity) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), activity.getString(R.string.breadcrumb_screen_shown), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logBackPress(activity: Activity) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), activity.getString(R.string.breadcrumb_screen_back_press), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logTooltipShown(context: Context, anchor: View) {
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), context.getString(R.string.breadcrumb_tooltip_shown_on_view, BreadCrumbViewUtil.getReadableNameForView(anchor)), WikipediaApp.instance.languageState.appLanguageCode))
        }

        fun logSettingsSelection(context: Context, title: String) {
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), context.getString(R.string.breadcrumb_view_click, title), WikipediaApp.instance.languageState.appLanguageCode))
        }
    }
}
