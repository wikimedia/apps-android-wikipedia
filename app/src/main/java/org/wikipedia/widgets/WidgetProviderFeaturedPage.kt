package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.os.BundleCompat
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.imageservice.ImageService
import java.util.concurrent.TimeUnit

class WidgetProviderFeaturedPage : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderFeaturedPage::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (widgetId in allWidgetIds) {
            L.d("updating widget...")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_featured_page)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)

            var pageTitle: PageTitle? = null
            val bundle = BundleCompat.getParcelable(options, Constants.ARG_TITLE, Bundle::class.java)
            if (bundle != null) {
                bundle.classLoader = WikipediaApp.instance.classLoader
                pageTitle = BundleCompat.getParcelable(bundle, Constants.ARG_TITLE, PageTitle::class.java)
            }
            if (pageTitle == null || (System.currentTimeMillis() - lastServerUpdateMillis) > TimeUnit.HOURS.toMillis(1)) {
                lastServerUpdateMillis = System.currentTimeMillis()
                WorkManager.getInstance(context).cancelAllWorkByTag(WidgetFeaturedPageWorker::class.java.simpleName)
                val workRequest = OneTimeWorkRequest.Builder(WidgetFeaturedPageWorker::class.java)
                    .addTag(WidgetFeaturedPageWorker::class.java.simpleName)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
                return
            }

            remoteViews.setTextViewText(R.id.widget_content_title, StringUtil.fromHtml(pageTitle.displayText))
            if (pageTitle.description.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_content_description, View.GONE)
            } else {
                remoteViews.setTextViewText(R.id.widget_content_description, pageTitle.description)
                remoteViews.setViewVisibility(R.id.widget_content_description, View.VISIBLE)
            }
            if (pageTitle.thumbUrl.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.GONE)
            } else {
                ImageService.loadImage(context, pageTitle.thumbUrl, onSuccess = { bitmap ->
                    remoteViews.setImageViewBitmap(R.id.widget_content_thumbnail, bitmap)
                })
                remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.VISIBLE)
            }

            val historyEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_WIDGET)
            val pendingIntent = PendingIntentCompat.getActivity(context, 1,
                PageActivity.newIntentForNewTab(context, historyEntry, historyEntry.title)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.WIDGET)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT, false)

            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    companion object {
        private var lastServerUpdateMillis = 0L

        fun forceUpdateWidget(context: Context, pageTitle: PageTitle? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderFeaturedPage::class.java))
            ids.forEach { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                val bundle = Bundle(WikipediaApp.instance.classLoader)
                bundle.putParcelable(Constants.ARG_TITLE, pageTitle)
                options.putParcelable(Constants.ARG_TITLE, bundle)
                appWidgetManager.updateAppWidgetOptions(id, options)
            }
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, WidgetProviderFeaturedPage::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }
}
