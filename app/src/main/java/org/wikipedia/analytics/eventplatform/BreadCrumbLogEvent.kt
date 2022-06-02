package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.view.View
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.SettingsActivity

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumb_log_event/1.0.0")
class BreadCrumbLogEvent(private val screen_name: String,
                         private val action: String,
                         private val primary_language_code: String) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.breadcrumb_log_event"

        fun logClick(activity: Activity?, view: View?) {
            activity?.let {
                if (it is SettingsActivity) {
                    return
                }
                val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
                if (viewReadableName != it.getString(R.string.breadcrumb_view_unnamed)) {
                    EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(it), it.getString(R.string.breadcrumb_view_click, viewReadableName), WikipediaApp.instance.languageState.appLanguageCode))
                }
            }
        }

        fun logLongClick(activity: Activity?, view: View?) {
            activity?.let {
                val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
                if (viewReadableName != it.getString(R.string.breadcrumb_view_unnamed)) {
                    EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(it), it.getString(R.string.breadcrumb_view_long_click, viewReadableName), WikipediaApp.instance.languageState.appLanguageCode))
                }
            }
        }

        fun logSwipe(activity: Activity?, isRtlSwipe: Boolean) {
            activity?.let {
                EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(it), it.getString(if (isRtlSwipe) R.string.breadcrumb_screen_swiped_left_on else R.string.breadcrumb_screen_swiped_right_on), WikipediaApp.instance.languageState.appLanguageCode))
            }
        }

        fun logScreenShown(activity: Activity?) {
            activity?.let {
                EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(it), it.getString(R.string.breadcrumb_screen_shown), WikipediaApp.instance.languageState.appLanguageCode))
            }
        }

        fun logBackPress(activity: Activity?) {
            activity?.let {
                EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(it), it.getString(R.string.breadcrumb_screen_back_press), WikipediaApp.instance.languageState.appLanguageCode))
            }
        }

        fun logTooltipShown(activity: Activity?, anchor: View) {
            activity?.let {
                EventPlatformClient.submit(BreadCrumbLogEvent(it.javaClass.simpleName.orEmpty(), it.getString(R.string.breadcrumb_tooltip_shown_on_view, BreadCrumbViewUtil.getReadableNameForView(anchor)), WikipediaApp.instance.languageState.appLanguageCode))
            }
        }

        fun logSettingsSelection(activity: Activity?, title: String?) {
            activity?.let {
                EventPlatformClient.submit(BreadCrumbLogEvent(it.javaClass.simpleName.orEmpty(), it.getString(R.string.breadcrumb_view_click, title), WikipediaApp.instance.languageState.appLanguageCode))
            }
        }
    }
}
