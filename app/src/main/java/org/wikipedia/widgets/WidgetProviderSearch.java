package org.wikipedia.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import org.wikipedia.R;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.search.SearchInvokeSource;

public class WidgetProviderSearch extends AppWidgetProvider {
    private static final String TAG = "WidgetSearch";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetProviderSearch.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            Log.d(TAG, "updating widget...");
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_search);

            // Create a PendingIntent to act as the onClickListener
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    SearchActivity.newIntent(context, SearchInvokeSource.WIDGET.code(), null),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // If we want to update the widget itself from the click event, then do something like this:
            //Intent intent = new Intent(context, WidgetProviderSearch.class);
            //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            //PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
