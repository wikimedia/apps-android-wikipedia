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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.AppWidgetTarget
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
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
            appWidgetManager.updateAppWidgetOptions(widgetId, options)

            var pageTitle: PageTitle? = null
            val bundle = BundleCompat.getParcelable(options, Constants.ARG_TITLE, Bundle::class.java)
            if (bundle != null) {
                bundle.classLoader = WikipediaApp.instance.classLoader
                pageTitle = BundleCompat.getParcelable(bundle, Constants.ARG_TITLE, PageTitle::class.java)
            }
            if (pageTitle == null) {
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
                Glide.with(context).asBitmap()
                    .load(pageTitle.thumbUrl)
                    .override(256)
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .transform(CenterCrop(), RoundedCorners(DimenUtil.roundedDpToPx(8f)))
                    .into(AppWidgetTarget(context, R.id.widget_content_thumbnail, remoteViews, widgetId))

                remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.VISIBLE)
            }

            appWidgetManager.updateAppWidget(widgetId, remoteViews)

            val pendingIntent = PendingIntentCompat.getActivity(context, 1,
                Intent(context, PageActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET, true),
                    PendingIntent.FLAG_UPDATE_CURRENT, false)

            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    companion object {
        fun forceUpdateWidget(context: Context, pageTitle: PageTitle? = null) {
            val intent = Intent(context, WidgetProviderFeaturedPage::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderFeaturedPage::class.java))
            if (ids.isNotEmpty()) {
                ids.forEach { id ->
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val bundle = Bundle(WikipediaApp.instance.classLoader)
                    bundle.putParcelable(Constants.ARG_TITLE, pageTitle)
                    options.putParcelable(Constants.ARG_TITLE, bundle)
                    appWidgetManager.updateAppWidgetOptions(id, options)
                }
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
