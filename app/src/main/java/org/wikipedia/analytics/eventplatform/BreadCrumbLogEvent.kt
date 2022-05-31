package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.util.Log
import android.view.View
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.settings.SettingsActivity

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumb_log_event/1.0.0")
class BreadCrumbLogEvent(private val screen_name: String,
                         private val action: String,
                         private val primary_language_code: String) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.breadcrumb_log_event"

        fun logClick(activity: BaseActivity, view: View?) {
            if (activity is SettingsActivity) {
                return
            }
            // EventPlatformClient.submit(BreadCrumbLogEvent(screenName, BreadCrumbViewUtil.getLogNameForView(view), WikipediaApp.getInstance().language().appLanguageCode))
            val viewReadableName = BreadCrumbViewUtil.getLogNameForView(view)
            if (viewReadableName != activity.getString(R.string.breadcrumb_view_unnamed)) {
                Log.e("|BREADCRUMB|", "|SCREEN|\t|" + BreadCrumbViewUtil.getReadableScreenName(activity) + "|\t|ACTION|\t|" + activity.getString(R.string.breadcrumb_view_click, viewReadableName) + "|")
            }
        }

        fun logSwipe(activity: BaseActivity, isRtlSwipe: Boolean) {
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + BreadCrumbViewUtil.getReadableScreenName(activity) + "|\t|ACTION|\t|" + activity.getString(if (isRtlSwipe) R.string.breadcrumb_screen_swiped_left_on else R.string.breadcrumb_screen_swiped_right_on) + "|")
        }

        fun logScreenShown(activity: BaseActivity) {
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + BreadCrumbViewUtil.getReadableScreenName(activity) + "|\t|ACTION|\t|" + activity.getString(R.string.breadcrumb_screen_shown) + "|")
        }

        fun logBackPress(activity: BaseActivity) {
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + BreadCrumbViewUtil.getReadableScreenName(activity) + "|\t|ACTION|\t|" + activity.getString(R.string.breadcrumb_screen_back_press) + "|")
        }

        fun logTooltipShown(activity: Activity, anchor: View) {
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + activity.javaClass.simpleName + "|\t|ACTION|\t|" + activity.getString(R.string.breadcrumb_tooltip_shown_on_view, BreadCrumbViewUtil.getLogNameForView(anchor)) + "|")
        }

        fun logSettingsSelection(activity: Activity, title: String?) {
            Log.e("|BREADCRUMB|", "|SCREEN|\t|" + activity.javaClass.simpleName + "|\t|ACTION|\t|" + activity.getString(R.string.breadcrumb_view_click, title) + "|")
        }
    }
}
