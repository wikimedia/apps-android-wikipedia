package org.wikipedia.views

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.util.MathUtil.percentage

class NotificationWithProgressBar {
    lateinit var notificationCategory: NotificationCategory
    lateinit var targetClass: Class<*>
    var notificationId = 0
    var notificationTitle = 0
    var notificationDescription = 0
    var isEnableCancelButton = false
    var isEnablePauseButton = false
    var isCanceled = false
    var isPaused = false

    fun setNotificationProgress(context: Context, itemsTotal: Int, itemsProgress: Int) {
        isCanceled = false
        isPaused = false
        val builder = NotificationCompat.Builder(context, notificationCategory.id)
        build(context, builder, itemsTotal, itemsProgress)
        builder.setProgress(itemsTotal, itemsProgress, itemsProgress == 0)
        showNotification(context, builder)
    }

    fun setNotificationPaused(context: Context, itemsTotal: Int, itemsProgress: Int) {
        val builder = NotificationCompat.Builder(context, notificationCategory.id)
        build(context, builder, itemsTotal, itemsProgress)
        builder.setProgress(itemsTotal, itemsProgress, true)
        showNotification(context, builder)
    }

    private fun build(context: Context, builder: NotificationCompat.Builder, total: Int, progress: Int) {
        val builderIcon = notificationCategory.iconResId
        val builderTitle = context.resources.getQuantityString(notificationTitle, total, total)
        val builderInfo = "${percentage(progress.toFloat(), total.toFloat()).toInt()}%"
        val builderDescription = context.resources.getQuantityString(notificationDescription, total - progress, total - progress)
        builder.setSmallIcon(builderIcon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, builderIcon))
                .setStyle(NotificationCompat.BigTextStyle().bigText(builderDescription))
                .setContentTitle(builderTitle)
                .setContentText(builderDescription)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null)
                .setContentInfo(builderInfo)
                .setOngoing(true)

        // build action buttons
        if (isEnablePauseButton) {
            val actionPause = actionBuilder(context,
                    targetClass,
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME,
                    if (isPaused) R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp,
                    if (isPaused) R.string.notification_syncing_resume_button else R.string.notification_syncing_pause_button,
                    0)
            builder.addAction(actionPause)
        }
        if (isEnableCancelButton) {
            val actionCancel = actionBuilder(context,
                    targetClass,
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL,
                    R.drawable.ic_close_black_24dp,
                    R.string.notification_syncing_cancel_button,
                    1)
            builder.addAction(actionCancel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(builderInfo)
        }
    }

    fun cancelNotification(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(notificationId)
    }

    private fun showNotification(context: Context, builder: NotificationCompat.Builder) {
        if (!isCanceled) {
            context.getSystemService<NotificationManager>()?.notify(notificationId, builder.build())
        }
    }

    private fun actionBuilder(context: Context,
                              targetClass: Class<*>,
                              intentExtra: String,
                              @DrawableRes buttonDrawable: Int,
                              @StringRes buttonText: Int,
                              requestCode: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(buttonDrawable, context.getString(buttonText),
                pendingIntentBuilder(context, targetClass, intentExtra, requestCode)).build()
    }

    private fun pendingIntentBuilder(context: Context,
                                     targetClass: Class<*>,
                                     intentExtra: String,
                                     requestCode: Int): PendingIntent {
        val resultIntent = Intent(context, targetClass)
        resultIntent.putExtra(intentExtra, true)
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getBroadcast(context, requestCode,
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
