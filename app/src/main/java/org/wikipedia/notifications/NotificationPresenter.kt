package org.wikipedia.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.notifications.db.Notification
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationPresenter {

    private var lastPermissionRequestTime = 0L

    fun maybeRequestPermission(context: Context, launcher: ActivityResultLauncher<String>) {
        val millisSinceLastRequest = System.currentTimeMillis() - lastPermissionRequestTime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Prefs.isInitialOnboardingEnabled &&
            AccountUtil.isLoggedIn &&
            (millisSinceLastRequest > TimeUnit.HOURS.toMillis(1) || millisSinceLastRequest < 0) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            lastPermissionRequestTime = System.currentTimeMillis()
        }
    }

    fun showNotification(context: Context, n: Notification, wikiSiteName: String, lang: String) {
        val notificationCategory = NotificationCategory.find(n.category)
        var activityIntent = addIntentExtras(NotificationActivity.newIntent(context), n.id, n.type)
        val builder = getDefaultBuilder(context, n.id, n.type, notificationCategory)
        val title = RichTextUtil.stripHtml(n.contents?.header.orEmpty())
        val id = n.key().toInt()

        n.contents?.links?.let {
            it.getPrimary()?.let { primary ->
                if (NotificationCategory.EDIT_USER_TALK.id == n.category) {
                    val talkWiki = WikiSite(primary.url)
                    val talkTitle = PageTitle.titleForUri(Uri.parse(primary.url), talkWiki)
                    activityIntent = addIntentExtras(TalkTopicsActivity.newIntent(context, talkTitle, Constants.InvokeSource.NOTIFICATION), n.id, n.type)
                    addActionForTalkPage(context, builder, primary, n)
                } else {
                    addAction(context, builder, primary, n)
                }
            }
            it.secondary?.let { secondary ->
                if (secondary.isNotEmpty()) {
                    addAction(context, builder, secondary[0], n)
                }
                if (secondary.size > 1) {
                    addAction(context, builder, secondary[1], n)
                }
            }
        }

        val themedContext = ContextThemeWrapper(context, if (WikipediaApp.instance.currentTheme == Theme.LIGHT) R.style.AppTheme else WikipediaApp.instance.currentTheme.resourceId)

        showNotification(context, builder, id, n.agent?.name ?: wikiSiteName, title, title, lang,
                notificationCategory.iconResId, ResourceUtil.getThemedAttributeId(themedContext, notificationCategory.iconColor), activityIntent)
    }

    fun showMultipleUnread(context: Context, unreadCount: Int) {
        // When showing the multiple-unread notification, we pass the unreadCount as the "id"
        // purely for analytics purposes, to get a sense of how many unread notifications are
        // typically queued up when the user has more than two of them.
        val builder = getDefaultBuilder(context, unreadCount.toLong(), NotificationPollBroadcastReceiver.TYPE_MULTIPLE)
        showNotification(context, builder, 0, context.getString(R.string.app_name),
                context.getString(R.string.notification_many_unread, unreadCount), context.getString(R.string.notification_many_unread, unreadCount),
                null, R.drawable.ic_notifications_black_24dp, R.color.blue600,
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
                         title: String, text: String, longText: CharSequence, lang: String?,
                         @DrawableRes icon: Int, @ColorRes color: Int, bodyIntent: Intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        builder.setContentIntent(PendingIntentCompat.getActivity(context, 0, bodyIntent, PendingIntent.FLAG_UPDATE_CURRENT, false))
                .setLargeIcon(drawNotificationBitmap(context, color, icon, lang.orEmpty().uppercase(Locale.getDefault())))
                .setSmallIcon(R.drawable.ic_wikipedia_w)
                .setColor(ContextCompat.getColor(context, color))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun addAction(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        if (UriUtil.isDiffUrl(link.url)) {
            try {
                addActionForDiffLink(context, builder, link, n)
                return
            } catch (e: Exception) {
                L.e(e)
            }
        }
        val pendingIntent = PendingIntentCompat.getActivity(context, 0,
            addIntentExtras(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)), n.id, n.type),
            PendingIntent.FLAG_UPDATE_CURRENT, false)
        val labelStr: String = if (link.tooltip.isNotEmpty()) {
            StringUtil.fromHtml(link.tooltip).toString()
        } else {
            StringUtil.fromHtml(link.label).toString()
        }
        builder.addAction(0, labelStr, pendingIntent)
    }

    private fun addActionForDiffLink(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        val uri = Uri.parse(link.url)
        val title = uri.getQueryParameter("title")
        val oldRev = uri.getQueryParameter("prev").orEmpty().toLongOrNull() ?: -1
        val newRev = uri.getQueryParameter("diff").orEmpty().toLongOrNull() ?: -1

        val pendingIntent = PendingIntentCompat.getActivity(context, 0,
            addIntentExtras(ArticleEditDetailsActivity.newIntent(context, PageTitle(title, WikiSite(link.url)), -1, oldRev, newRev), n.id, n.type),
            PendingIntent.FLAG_UPDATE_CURRENT, false)
        builder.addAction(0, StringUtil.fromHtml(link.label).toString(), pendingIntent)
    }

    private fun addActionForTalkPage(context: Context, builder: NotificationCompat.Builder, link: Notification.Link, n: Notification) {
        val wiki = WikiSite(link.url)
        val title = PageTitle.titleForUri(Uri.parse(link.url), wiki)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0,
            addIntentExtras(TalkTopicsActivity.newIntent(context, title, Constants.InvokeSource.NOTIFICATION), n.id, n.type),
            PendingIntent.FLAG_UPDATE_CURRENT, false)
        builder.addAction(0, StringUtil.fromHtml(link.label).toString(), pendingIntent)
    }

    private fun addActionWithDirectReply(context: Context, builder: NotificationCompat.Builder,
                                         title: PageTitle, replyTo: String, id: Int) {
        val remoteInput = RemoteInput.Builder(NotificationPollBroadcastReceiver.RESULT_KEY_DIRECT_REPLY)
            .build()
        val resultIntent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_DIRECT_REPLY)
            .putExtra(Constants.ARG_WIKISITE, title.wikiSite)
            .putExtra(Constants.ARG_TITLE, title)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_REPLY_TO, replyTo)
            .putExtra(NotificationPollBroadcastReceiver.RESULT_EXTRA_ID, id)
        val resultPendingIntent = PendingIntentCompat.getBroadcast(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT, false)

        val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_24, context.getString(R.string.notifications_direct_reply_action), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
        builder.addAction(action)
    }

    fun drawNotificationBitmap(context: Context, @ColorRes color: Int, @DrawableRes icon: Int, lang: String): Bitmap {
        val bitmapHalfSize = DimenUtil.roundedDpToPx(24f)
        val iconHalfSize = DimenUtil.roundedDpToPx(14f)
        return createBitmap(bitmapHalfSize * 2, bitmapHalfSize * 2).applyCanvas {
            val p = Paint()
            p.isAntiAlias = true
            p.color = ContextCompat.getColor(context, color)

            if (lang.isNotEmpty()) {
                p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                p.textSize = DimenUtil.dpToPx(12f)
                p.strokeWidth = DimenUtil.dpToPx(1f)

                val textBounds = Rect()
                p.getTextBounds(lang, 0, lang.length, textBounds)

                val rectPadding = DimenUtil.dpToPx(4f)
                val textLeft = bitmapHalfSize.toFloat() - (textBounds.right - textBounds.left) / 2
                val textBottom = bitmapHalfSize * 2 - rectPadding - p.strokeWidth

                drawText(lang, textLeft, textBottom, p)

                p.style = Paint.Style.STROKE
                val rBounds = RectF(textLeft + textBounds.left - rectPadding, textBottom + textBounds.top - rectPadding,
                        textLeft + textBounds.right + rectPadding, textBottom + textBounds.bottom + rectPadding)
                drawRoundRect(rBounds, rectPadding, rectPadding, p)
            }

            val iconBmp = ResourceUtil.bitmapFromVectorDrawable(context, icon, color)
            drawBitmap(iconBmp, null, Rect(bitmapHalfSize - iconHalfSize, 0,
                    bitmapHalfSize + iconHalfSize, iconHalfSize * 2), null)
            iconBmp.recycle()
        }
    }
}
