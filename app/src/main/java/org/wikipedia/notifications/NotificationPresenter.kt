package org.wikipedia.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

object NotificationPresenter {

    fun showNotification(context: Context, n: Notification, wikiSiteName: String) {
        val notificationCategory = NotificationCategory.find(n.category)
        val activityIntent = addIntentExtras(NotificationActivity.newIntent(context), n.id, n.type)
        val builder = getDefaultBuilder(context, n.id, n.type, notificationCategory)
        val title: String = StringUtil.fromHtml(if (n.contents != null) n.contents.header else "").toString()
        val id = n.key().toInt()

        n.contents?.links?.let {
            it.getPrimary()?.let { primary ->
                if (NotificationCategory.EDIT_USER_TALK.id == n.category) {
                    addActionWithDirectReply(context, builder, primary, n.agent?.name.orEmpty(), id)
                } else {
                    addAction(context, builder, primary, n)
                }
            }
            it.secondary?.let { secondary ->
                if (secondary.size > 0) {
                    addAction(context, builder, secondary[0], n)
                }
                if (secondary.size > 1) {
                    addAction(context, builder, secondary[1], n)
                }
            }
        }

        showNotification(context, builder, id, wikiSiteName, title, title, notificationCategory.iconResId, notificationCategory.iconColor, true, activityIntent)
    }

    fun showMultipleUnread(context: Context, unreadCount: Int) {
        // When showing the multiple-unread notification, we pass the unreadCount as the "id"
        // purely for analytics purposes, to get a sense of how many unread notifications are
        // typically queued up when the user has more than two of them.
        val builder = getDefaultBuilder(context, unreadCount.toLong(), NotificationPollBroadcastReceiver.TYPE_MULTIPLE)
        showNotification(context, builder, 0, context.getString(R.string.app_name),
                context.getString(R.string.notification_many_unread, unreadCount), context.getString(R.string.notification_many_unread, unreadCount),
                R.drawable.ic_notifications_black_24dp, R.color.accent50, true,
                addIntentExtras(NotificationActivity.newIntent(context), unreadCount.toLong(), NotificationPollBroadcastReceiver.TYPE_MULTIPLE))
    }

    fun addIntentExtras(intent: Intent, id: Long, type: String): Intent {
        return intent.putExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, id)
                .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE, type)
    }

    fun getDefaultBuilder(context: Context, id: Long, type: String?, notificationCategory: NotificationCategory = NotificationCategory.SYSTEM): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, notificationCategory.id)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDeleteIntent(NotificationPollBroadcastReceiver.getCancelNotificationPendingIntent(context, id, type))
    }

    fun showNotification(context: Context, builder: NotificationCompat.Builder, id: Int,
                         title: String, text: String, longText: CharSequence,
                         @DrawableRes icon: Int, @ColorRes color: Int, drawIconCircle: Boolean, bodyIntent: Intent) {
        builder.setContentIntent(PendingIntent.getActivity(context, 0, bodyIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLargeIcon(drawNotificationBitmap(context, color, icon, drawIconCircle))
                .setSmallIcon(R.drawable.ic_wikipedia_w)
                .setColor(ContextCompat.getColor(context, color))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
        context.getSystemService<NotificationManager>()?.notify(id, builder.build())
    }

    private fun addAction(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        val pendingIntent = PendingIntent.getActivity(context, 0,
                addIntentExtras(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)), n.id, n.type), PendingIntent.FLAG_UPDATE_CURRENT)
        val labelStr: String = if (link.tooltip.isNotEmpty()) {
            StringUtil.fromHtml(link.tooltip).toString()
        } else {
            StringUtil.fromHtml(link.label).toString()
        }
        builder.addAction(0, labelStr, pendingIntent)
    }

    private fun addActionWithDirectReply(context: Context, builder: NotificationCompat.Builder,
                                         link: Notification.Link, replyTo: String, id: Int) {
        val wiki = WikiSite(link.url)
        val title = wiki.titleForUri(Uri.parse(link.url))

        val remoteInput = RemoteInput.Builder(NotificationPollBroadcastReceiver.RESULT_KEY_DIRECT_REPLY)
            .build()
        val resultIntent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_DIRECT_REPLY)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_WIKI, wiki)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_TITLE, title)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_REPLY_TO, replyTo)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_ID, id)
        val resultPendingIntent = PendingIntent.getBroadcast(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_24, context.getString(R.string.notifications_direct_reply_action), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
        builder.addAction(action)
    }

    private fun drawNotificationBitmap(context: Context, @ColorRes color: Int, @DrawableRes icon: Int, drawIconCircle: Boolean): Bitmap {
        val bitmapHalfSize = DimenUtil.roundedDpToPx(20f)
        val iconHalfSize = DimenUtil.roundedDpToPx(12f)
        return createBitmap(bitmapHalfSize * 2, bitmapHalfSize * 2).applyCanvas {
            val p = Paint()
            p.isAntiAlias = true
            p.color = ContextCompat.getColor(context, if (drawIconCircle) color else android.R.color.transparent)
            drawCircle(bitmapHalfSize.toFloat(), bitmapHalfSize.toFloat(), bitmapHalfSize.toFloat(), p)
            val iconBmp = ResourceUtil.bitmapFromVectorDrawable(context, icon, if (drawIconCircle) android.R.color.white else color)
            drawBitmap(iconBmp, null, Rect(bitmapHalfSize - iconHalfSize, bitmapHalfSize - iconHalfSize,
                    bitmapHalfSize + iconHalfSize, bitmapHalfSize + iconHalfSize), null)
            iconBmp.recycle()
        }
    }
}
