package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Checkable
import androidx.appcompat.widget.SwitchCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.log.L

@Suppress("unused", "CanBeParameter")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumbs_event/1.0.0")
class BreadCrumbLogEvent(
        private val screen_name: String,
        private val action: String,
        private val app_primary_language_code: String = WikipediaApp.instance.languageState.appLanguageCode
) : MobileAppsEvent(STREAM_NAME) {

    init {
        L.d(">>> $screen_name.$action")
    }

    companion object {
        private const val STREAM_NAME = "android.breadcrumbs_event"

        fun logClick(activity: Activity, view: View) {
            if (activity is SettingsActivity) {
                return
            }
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            val str = viewReadableName + "." + if (view is SwitchCompat) (if (!view.isChecked) "on" else "off") else "click"
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), str))
        }

        fun logLongClick(activity: Activity, view: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), "$viewReadableName.longclick"))
        }

        fun logScreenShown(activity: Activity) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), "show"))
        }

        fun logBackPress(activity: Activity) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(activity), "back"))
        }

        fun logTooltipShown(context: Context, anchor: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(anchor)
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), "$viewReadableName.tooltip"))
        }

        fun logSettingsSelection(context: Context, title: String, newValue: Any? = null) {
            val str = title + "." + if (newValue is Boolean) (if (newValue == true) "on" else "off") else "click"
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), str))
        }
    }
}
