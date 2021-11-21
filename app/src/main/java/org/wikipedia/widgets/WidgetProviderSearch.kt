package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.search.SearchActivity
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.log.L

class WidgetProviderSearch : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderSearch::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (widgetId in allWidgetIds) {
            L.d("updating widget...")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_search)
            val pendingIntent = PendingIntent.getActivity(context, 0,
                    SearchActivity.newIntent(context, InvokeSource.WIDGET, null),
                    PendingIntent.FLAG_UPDATE_CURRENT or DeviceUtil.pendingIntentFlags)
            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }
}
