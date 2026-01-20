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
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.imageservice.ImageService
import java.util.concurrent.TimeUnit

class WidgetProviderRabbitHole : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderRabbitHole::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (widgetId in allWidgetIds) {
            L.d("updating rabbit hole widget...")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_rabbit_hole)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)

            var startPageTitle: PageTitle? = null
            var endPageTitle: PageTitle? = null

            val bundle = BundleCompat.getParcelable(options, ARG_RABBIT_HOLE_DATA, Bundle::class.java)
            if (bundle != null) {
                bundle.classLoader = WikipediaApp.instance.classLoader
                startPageTitle = BundleCompat.getParcelable(bundle, ARG_START_TITLE, PageTitle::class.java)
                endPageTitle = BundleCompat.getParcelable(bundle, ARG_END_TITLE, PageTitle::class.java)
            }

            if (startPageTitle == null || endPageTitle == null ||
                (System.currentTimeMillis() - lastServerUpdateMillis) > TimeUnit.HOURS.toMillis(1)) {
                lastServerUpdateMillis = System.currentTimeMillis()
                WorkManager.getInstance(context).cancelAllWorkByTag(WidgetRabbitHoleWorker::class.java.simpleName)
                val workRequest = OneTimeWorkRequest.Builder(WidgetRabbitHoleWorker::class.java)
                    .addTag(WidgetRabbitHoleWorker::class.java.simpleName)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
                return
            }

            // Set up starting article
            remoteViews.setTextViewText(R.id.widget_start_title, StringUtil.fromHtml(startPageTitle.displayText))
            if (startPageTitle.description.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_start_description, View.GONE)
            } else {
                remoteViews.setTextViewText(R.id.widget_start_description, startPageTitle.description)
                remoteViews.setViewVisibility(R.id.widget_start_description, View.VISIBLE)
            }
            if (startPageTitle.thumbUrl.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_start_thumbnail, View.GONE)
            } else {
                ImageService.loadImage(context, ImageUrlUtil.getUrlForPreferredSize(startPageTitle.thumbUrl!!,
                    Service.PREFERRED_THUMB_SIZE), onSuccess = { bitmap ->
                    remoteViews.setImageViewBitmap(R.id.widget_start_thumbnail, bitmap)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                })
                remoteViews.setViewVisibility(R.id.widget_start_thumbnail, View.VISIBLE)
            }

            // Set up ending article
            remoteViews.setTextViewText(R.id.widget_end_title, StringUtil.fromHtml(endPageTitle.displayText))
            if (endPageTitle.description.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_end_description, View.GONE)
            } else {
                remoteViews.setTextViewText(R.id.widget_end_description, endPageTitle.description)
                remoteViews.setViewVisibility(R.id.widget_end_description, View.VISIBLE)
            }
            if (endPageTitle.thumbUrl.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_end_thumbnail, View.GONE)
            } else {
                ImageService.loadImage(context, ImageUrlUtil.getUrlForPreferredSize(endPageTitle.thumbUrl!!,
                    Service.PREFERRED_THUMB_SIZE), onSuccess = { bitmap ->
                    remoteViews.setImageViewBitmap(R.id.widget_end_thumbnail, bitmap)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                })
                remoteViews.setViewVisibility(R.id.widget_end_thumbnail, View.VISIBLE)
            }

            // Set click listener for starting article
            val startHistoryEntry = HistoryEntry(startPageTitle, HistoryEntry.SOURCE_WIDGET)
            val startPendingIntent = PendingIntentCompat.getActivity(context, PENDING_INTENT_START_ARTICLE,
                PageActivity.newIntentForNewTab(context, startHistoryEntry, startHistoryEntry.title)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.WIDGET)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT, false)
            remoteViews.setOnClickPendingIntent(R.id.widget_start_article_container, startPendingIntent)

            // Set click listener for ending article
            val endHistoryEntry = HistoryEntry(endPageTitle, HistoryEntry.SOURCE_WIDGET)
            val endPendingIntent = PendingIntentCompat.getActivity(context, PENDING_INTENT_END_ARTICLE,
                PageActivity.newIntentForNewTab(context, endHistoryEntry, endHistoryEntry.title)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.WIDGET)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT, false)
            remoteViews.setOnClickPendingIntent(R.id.widget_end_article_container, endPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    companion object {
        private const val ARG_RABBIT_HOLE_DATA = "rabbitHoleData"
        private const val ARG_START_TITLE = "startTitle"
        private const val ARG_END_TITLE = "endTitle"
        private const val PENDING_INTENT_START_ARTICLE = 100
        private const val PENDING_INTENT_END_ARTICLE = 101
        private var lastServerUpdateMillis = 0L

        fun forceUpdateWidget(context: Context, startPageTitle: PageTitle? = null, endPageTitle: PageTitle? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderRabbitHole::class.java))
            ids.forEach { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                val bundle = Bundle(WikipediaApp.instance.classLoader)
                bundle.putParcelable(ARG_START_TITLE, startPageTitle)
                bundle.putParcelable(ARG_END_TITLE, endPageTitle)
                options.putParcelable(ARG_RABBIT_HOLE_DATA, bundle)
                appWidgetManager.updateAppWidgetOptions(id, options)
            }
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, WidgetProviderRabbitHole::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }
}
