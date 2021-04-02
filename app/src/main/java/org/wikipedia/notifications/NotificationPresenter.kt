package org.wikipedia.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

object NotificationPresenter {
    private const val CHANNEL_ID = "MEDIAWIKI_ECHO_CHANNEL"

    fun showNotification(context: Context, n: Notification, wikiSiteName: String) {
        @DrawableRes var iconResId = R.drawable.ic_speech_bubbles
        @ColorRes var iconColor = R.color.accent50

        val builder = getDefaultBuilder(context, n.id(), n.type())
        val title: String = StringUtil.fromHtml(if (n.contents != null) n.contents!!.header else "").toString()

        n.contents?.links?.let {
            it.primary?.let { primary ->
                if (Notification.CATEGORY_EDIT_USER_TALK == n.category()) {
                    addActionForTalkPage(context, builder, primary, n)
                } else {
                    addAction(context, builder, primary, n)
                }
            }
            it.secondary?.let { secondary ->
                if (secondary.size > 0) {
                    addAction(context, builder, secondary[0], n)
                } else if (secondary.size > 1) {
                    addAction(context, builder, secondary[1], n)
                }
            }
        }

        val activityIntent = addIntentExtras(NotificationActivity.newIntent(context), n.id(), n.type())
        val s = n.category()
        when {
            Notification.CATEGORY_EDIT_USER_TALK == s -> {
                iconResId = R.drawable.ic_edit_user_talk
                iconColor = R.color.accent50
            }
            Notification.CATEGORY_REVERTED == s -> {
                iconResId = R.drawable.ic_revert
                iconColor = R.color.base20
            }
            Notification.CATEGORY_EDIT_THANK == s -> {
                iconResId = R.drawable.ic_user_talk
                iconColor = R.color.green50
            }
            Notification.CATEGORY_THANK_YOU_EDIT == s -> {
                iconResId = R.drawable.ic_edit_progressive
                iconColor = R.color.accent50
            }
            s.startsWith(Notification.CATEGORY_MENTION) -> {
                iconResId = R.drawable.ic_mention
                iconColor = R.color.accent50
            }
            Notification.CATEGORY_LOGIN_FAIL == s -> {
                iconResId = R.drawable.ic_user_avatar
                iconColor = R.color.base0
            }
        }
        showNotification(context, builder, n.key().toInt(), wikiSiteName, title, title, iconResId, iconColor, true, activityIntent)
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

    fun getDefaultBuilder(context: Context, id: Long, type: String?): NotificationCompat.Builder {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                notificationChannel = NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.notification_echo_channel_description), importance)
                notificationChannel.lightColor = ContextCompat.getColor(context, R.color.accent50)
                notificationChannel.enableVibration(true)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    private fun addAction(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        val pendingIntent = PendingIntent.getActivity(context, 0,
                addIntentExtras(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)), n.id(), n.type()), 0)
        val labelStr: String = if (link.tooltip.isNotEmpty()) {
            StringUtil.fromHtml(link.tooltip).toString()
        } else {
            StringUtil.fromHtml(link.label).toString()
        }
        builder.addAction(0, labelStr, pendingIntent)
    }

    private fun addActionForTalkPage(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        val wiki = WikiSite(link.url)
        val title = wiki.titleForUri(Uri.parse(link.url))
        val pendingIntent = PendingIntent.getActivity(context, 0,
                addIntentExtras(TalkTopicsActivity.newIntent(context, title.pageTitleForTalkPage(), Constants.InvokeSource.NOTIFICATION), n.id(), n.type()), 0)
        builder.addAction(0, StringUtil.fromHtml(link.label).toString(), pendingIntent)
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
