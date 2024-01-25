package org.wikipedia.analytics.eventplatform

import android.app.Activity
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.log.L

@Suppress("unused", "CanBeParameter")
@Serializable
@SerialName("/analytics/mobile_apps/android_breadcrumbs_event/1.0.0")
class BreadCrumbLogEvent(
        private val screen_name: String,
        private val action: String
) : MobileAppsEvent(STREAM_NAME) {

    // Do NOT join the declaration and assignment to these fields, or they won't be serialized correctly.
    private val app_primary_language_code: String

    init {
        app_primary_language_code = WikipediaApp.instance.languageState.appLanguageCode
        L.d(">>> $screen_name.$action")
    }

    companion object {
        private const val STREAM_NAME = "android.breadcrumbs_event"

        fun logClick(context: Context, view: View) {
            if (context is SettingsActivity) {
                return
            }
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            val str = "$viewReadableName." + when (view) {
                is Checkable -> if (!view.isChecked) "on" else "off"
                else -> "click"
            }
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), str))
        }

        fun logClick(context: Context, item: MenuItem) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context),
                context.resources.getResourceEntryName(item.itemId) + ".click"))
        }

        fun logLongClick(context: Context, view: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), "$viewReadableName.longclick"))
        }

        fun logScreenShown(context: Context, fragment: Fragment? = null) {
            val invokeSource = (fragment?.activity?.intent ?: (context as? Activity)?.intent)?.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as? Constants.InvokeSource
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context, fragment),
                "show" + (invokeSource?.let { ".from." + it.value } ?: "")))
        }

        fun logBackPress(context: Context) {
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), "back"))
        }

        fun logTooltipShown(context: Context, anchor: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(anchor)
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), "$viewReadableName.tooltip"))
        }

        fun logSettingsSelection(context: Context, title: String, newValue: Any? = null) {
            val str = title + "." + if (newValue is Boolean) (if (newValue == true) "on" else "off") else "click"
            EventPlatformClient.submit(BreadCrumbLogEvent(context.javaClass.simpleName.orEmpty(), str))
        }

        fun logInputField(context: Context, view: View) {
            val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
            val str = "$viewReadableName." + (view as TextView).text
            EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), str))
        }
    }
}
