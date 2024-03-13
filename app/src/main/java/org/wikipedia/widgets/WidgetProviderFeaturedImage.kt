package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.AppWidgetTarget
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.json.JsonUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit

class WidgetProviderFeaturedImage : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderFeaturedImage::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (widgetId in allWidgetIds) {
            L.d("updating widget...")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_featured_image)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)

            var image: FeaturedImage? = null
            options.getString(Constants.ARG_TITLE)?.let {
                image = JsonUtil.decodeFromString(it)
            }
            val fromWorker = options.getBoolean(EXTRA_UPDATE_FROM_WORKER, false)

            val diffMillis = System.currentTimeMillis() - lastServerUpdateMillis
            if (!fromWorker || image == null || diffMillis <= 0 ||
                diffMillis > TimeUnit.MINUTES.toMillis(1)) {
                lastServerUpdateMillis = System.currentTimeMillis()
                WorkManager.getInstance(context).cancelAllWorkByTag(WidgetFeaturedImageWorker::class.java.simpleName)
                val workRequest = OneTimeWorkRequest.Builder(WidgetFeaturedImageWorker::class.java)
                    .addTag(WidgetFeaturedImageWorker::class.java.simpleName)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)

                options.putBoolean(EXTRA_UPDATE_FROM_WORKER, false)
                appWidgetManager.updateAppWidgetOptions(widgetId, options)
                return
            }

            remoteViews.setTextViewText(R.id.widget_content_title, StringUtil.fromHtml(image?.description?.text))
            remoteViews.setTextViewText(R.id.widget_content_description, StringUtil.fromHtml(image?.artist?.text))

            Glide.with(context).asBitmap()
                .load(ImageUrlUtil.getUrlForPreferredSize(image!!.thumbnail.source, Constants.PREFERRED_GALLERY_IMAGE_SIZE))
                .override(512)
                .downsample(DownsampleStrategy.FIT_CENTER)
                // .transform(CenterCrop(), RoundedCorners(DimenUtil.roundedDpToPx(16f)))
                .into(AppWidgetTarget(context, R.id.widget_image, remoteViews, widgetId))

            remoteViews.setOnClickPendingIntent(R.id.widget_container, PendingIntentCompat.getActivity(context, 1,
                Intent(Intent.ACTION_VIEW, Uri.parse(Service.COMMONS_URL + "/wiki/" + image!!.title)),
                PendingIntent.FLAG_UPDATE_CURRENT, false))

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    class WidgetFeaturedImageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            return try {
                val date = DateUtil.getUtcRequestDateFor(0)
                val result = ServiceFactory.getRest(WikipediaApp.instance.wikiSite)
                    .getFeedFeatured(date.year, date.month, date.day)

                forceUpdateWidget(applicationContext, result.potd, fromWorker = true)
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

        fun forceUpdateWidget(context: Context, image: FeaturedImage? = null, fromWorker: Boolean = false) {
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderFeaturedImage::class.java))
            ids.forEach { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                options.putString(Constants.ARG_TITLE, if (image != null) JsonUtil.encodeToString(image) else null)
                options.putBoolean(EXTRA_UPDATE_FROM_WORKER, fromWorker)
                appWidgetManager.updateAppWidgetOptions(id, options)
            }
        }
    }
}
