package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.search.SearchActivity
import org.wikipedia.util.log.L

class WidgetProviderSearch : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderSearch::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (widgetId in allWidgetIds) {
            L.d("updating widget...")

            val pendingIntent = PendingIntentCompat.getActivity(context, 0,
                SearchActivity.newIntent(context, InvokeSource.WIDGET, null)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT, false)

            val smallView = RemoteViews(context.packageName, R.layout.widget_search_small)
            val largeView = RemoteViews(context.packageName, R.layout.widget_search_large)
            smallView.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            largeView.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            val viewMapping: Map<SizeF, RemoteViews> = mapOf(
                SizeF(32f, 32f) to smallView,
                SizeF(160f, 32f) to largeView
            )
            val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(viewMapping)
            } else {
                largeView
            }

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }
}
