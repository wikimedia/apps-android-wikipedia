package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.util.SizeFCompat
import androidx.core.widget.updateAppWidget
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.search.SearchActivity

class WidgetProviderSearch : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderSearch::class.java)
        for (widgetId in appWidgetManager.getAppWidgetIds(thisWidget)) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: Bundle) {
        // appWidgetManager.updateAppWidget must be called here on API levels < 31
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val small = SizeFCompat(32f, 32f)
        val medium = SizeFCompat(64f, 32f)
        val large = SizeFCompat(160f, 32f)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0,
            SearchActivity.newIntent(context, InvokeSource.WIDGET, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT, false)

        appWidgetManager.updateAppWidget(appWidgetId, listOf(small, medium, large)) {
            val remoteViewId = when (it) {
                small -> R.layout.widget_search_small
                medium -> R.layout.widget_search_medium
                else -> R.layout.widget_search_large
            }
            RemoteViews(context.packageName, remoteViewId)
                .apply { setOnClickPendingIntent(R.id.widget_container, pendingIntent) }
        }
    }
}
