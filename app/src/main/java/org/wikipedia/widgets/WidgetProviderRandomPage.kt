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
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.AppWidgetTarget
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit

class WidgetProviderRandomPage : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderRandomPage::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (widgetId in allWidgetIds) {
            L.d("updating widget...")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_random_page)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)

            var pageTitle: PageTitle? = null
            val bundle = BundleCompat.getParcelable(options, Constants.ARG_TITLE, Bundle::class.java)
            if (bundle != null) {
                bundle.classLoader = WikipediaApp.instance.classLoader
                pageTitle = BundleCompat.getParcelable(bundle, Constants.ARG_TITLE, PageTitle::class.java)
            }
            val fromWorker = options.getBoolean(EXTRA_UPDATE_FROM_WORKER, false)

            val diffMillis = System.currentTimeMillis() - lastServerUpdateMillis
            if (!fromWorker || pageTitle == null || diffMillis <= 0 ||
                diffMillis > TimeUnit.SECONDS.toMillis(10)) {
                lastServerUpdateMillis = System.currentTimeMillis()
                WorkManager.getInstance(context).cancelAllWorkByTag(WidgetRandomPageWorker::class.java.simpleName)
                val workRequest = OneTimeWorkRequest.Builder(WidgetRandomPageWorker::class.java)
                    .addTag(WidgetRandomPageWorker::class.java.simpleName)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)

                options.putBoolean(EXTRA_UPDATE_FROM_WORKER, false)
                appWidgetManager.updateAppWidgetOptions(widgetId, options)
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
                Glide.with(context).asBitmap()
                    .load(pageTitle.thumbUrl)
                    .override(256)
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .transform(CenterCrop(), RoundedCorners(DimenUtil.roundedDpToPx(16f)))
                    .into(AppWidgetTarget(context, R.id.widget_content_thumbnail, remoteViews, widgetId))

                remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.VISIBLE)
            }

            val historyEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_WIDGET)

            remoteViews.setOnClickPendingIntent(R.id.title_container, PendingIntentCompat.getActivity(context, 1,
                PageActivity.newIntentForNewTab(context, historyEntry, historyEntry.title)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.WIDGET)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT, false))

            remoteViews.setOnClickPendingIntent(R.id.logo_image, PendingIntentCompat.getBroadcast(context, 1,
                Intent(context, WidgetProviderRandomPage::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds),
                PendingIntent.FLAG_UPDATE_CURRENT, false))

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    class WidgetRandomPageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            return try {
                val result = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getRandom()
                val pageTitle = result.getPageTitle(WikipediaApp.instance.wikiSite)
                pageTitle.displayText = result.displayTitle

                forceUpdateWidget(applicationContext, pageTitle, fromWorker = true, sendIntent = false)

                Result.success()
            } catch (e: Exception) {
                L.e(e)
                Result.retry()
            }
        }
    }

    companion object {
        private var lastServerUpdateMillis = 0L
        private const val EXTRA_UPDATE_FROM_WORKER = "updateFromWorker"

        fun forceUpdateWidget(context: Context, pageTitle: PageTitle? = null, fromWorker: Boolean = false, sendIntent: Boolean = true) {
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderRandomPage::class.java))
            ids.forEach { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                val bundle = Bundle(WikipediaApp.instance.classLoader)
                bundle.putParcelable(Constants.ARG_TITLE, pageTitle)
                options.putParcelable(Constants.ARG_TITLE, bundle)
                options.putBoolean(EXTRA_UPDATE_FROM_WORKER, fromWorker)
                appWidgetManager.updateAppWidgetOptions(id, options)
            }
            if (ids.isNotEmpty() && sendIntent) {
                context.sendBroadcast(Intent(context, WidgetProviderRandomPage::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }
}
