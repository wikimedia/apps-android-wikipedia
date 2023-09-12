package org.wikipedia.analytics.metricsplatform

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.wikipedia.analytics.eventplatform.BreadCrumbViewUtil
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.log.L

class BreadcrumbLogEvent : MetricsEvent() {
    fun log(context: Context, logString: String) {
        submitEvent(BreadCrumbViewUtil.getReadableScreenName(context), logString)
    }

    fun logClick(context: Context, view: View) {
        if (context is SettingsActivity) {
            return
        }
        val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
        val str = "$viewReadableName." + when (view) {
            is Checkable -> if (!view.isChecked) "on" else "off"
            else -> "click"
        }
        submitEvent(BreadCrumbViewUtil.getReadableScreenName(context), str)
    }

    fun logClick(context: Context, item: MenuItem) {
        submitEvent(
            BreadCrumbViewUtil.getReadableScreenName(context),
            context.resources.getResourceEntryName(item.itemId) + ".click"
        )
    }

    fun logLongClick(context: Context, view: View) {
        val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
        submitEvent(
            BreadCrumbViewUtil.getReadableScreenName(context),
            "$viewReadableName.longclick"
        )
    }

    fun logScreenShown(context: Context, fragment: Fragment? = null) {
        submitEvent(BreadCrumbViewUtil.getReadableScreenName(context, fragment), "show")
    }

    fun logBackPress(context: Context) {
        submitEvent(BreadCrumbViewUtil.getReadableScreenName(context), "back")
    }

    fun logTooltipShown(context: Context, anchor: View) {
        val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(anchor)
        submitEvent(context.javaClass.simpleName.orEmpty(), "$viewReadableName.tooltip")
    }

    fun logSettingsSelection(context: Context, title: String, newValue: Any? = null) {
        val str =
            title + "." + if (newValue is Boolean) (if (newValue == true) "on" else "off") else "click"
        submitEvent(context.javaClass.simpleName.orEmpty(), str)
    }

    fun logInputField(context: Context, view: View) {
        val viewReadableName = BreadCrumbViewUtil.getReadableNameForView(view)
        val str = "$viewReadableName." + (view as TextView).text
        submitEvent(BreadCrumbViewUtil.getReadableScreenName(context), str)
    }

    private fun submitEvent(context: String, action: String) {
        L.d(">>> metrics_platform.breadcrumbs_event.$context.$action")
        submitEvent(
            "breadcrumbs_event.$context",
            mapOf(
                "action" to action
            )
        )
    }
}
