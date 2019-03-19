package org.wikipedia.editactionfeed.unlock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getColor
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity.Companion.EXTRA_SOURCE
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil


object SuggestedEditsUnlockNotifications {

    private const val NOTIFICATION_ID = 1003
    private const val NOTIFICATION_CHANNEL_ID = "SUGGESTED_EDITS_CHANNEL"
    private const val NOTIFICATION_ICON = R.drawable.ic_mode_edit_white_24dp
    private const val NOTIFICATION_COLOR = R.color.accent50

    private fun notificationBuilder(context: Context): NotificationCompat.Builder {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            var notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (notificationChannel == null) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.notification_echo_channel_description), importance)
                notificationChannel.lightColor = ContextCompat.getColor(context, R.color.accent50)
                notificationChannel.enableVibration(true)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(NOTIFICATION_ICON)
                .setLargeIcon(NotificationPresenter.drawNotificationBitmap(context, NOTIFICATION_COLOR, NOTIFICATION_ICON))
                .setSound(null)
                .setContentText(context.getString(R.string.suggested_edits_unlock_notification_text))
                .setColor(getColor(context, NOTIFICATION_COLOR))
    }

    private fun notificationActionBuilder(context: Context,
                                          targetClass: Class<*>,
                                          source: Constants.InvokeSource,
                                          buttonText: Int,
                                          requestCode: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(0, context.getString(buttonText), pendingIntentBuilder(context, targetClass, source, requestCode)).build()
    }

    private fun pendingIntentBuilder(context: Context,
                                     targetClass: Class<*>,
                                     source: Constants.InvokeSource,
                                     requestCode: Int): PendingIntent {
        val resultIntent = Intent(context, targetClass)
        resultIntent.putExtra(EXTRA_SOURCE, source)
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, requestCode,
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun showUnlockAddDescriptionNotification(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (Prefs.isSuggestedEditsAddDescriptionsUnlocked()
                || !ReleaseUtil.isPreBetaRelease()) {
            return
        }

        Prefs.setSuggestedEditsAddDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)

        val builder = notificationBuilder(context)

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_big_text)))
                .setContentTitle(context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_title))
                .setContentIntent(pendingIntentBuilder(context, AddTitleDescriptionsActivity::class.java, Constants.InvokeSource.EDIT_FEED_TITLE_DESC, 1))
                .addAction(notificationActionBuilder(context, AddTitleDescriptionsActivity::class.java,
                        Constants.InvokeSource.EDIT_FEED_TITLE_DESC,
                        R.string.suggested_edits_unlock_notification_button, 1))

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, builder.build())
    }

    fun showUnlockTranslateDescriptionNotification(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (WikipediaApp.getInstance().language().appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
                || Prefs.isSuggestedEditsTranslateDescriptionsUnlocked()
                || !ReleaseUtil.isPreBetaRelease()) {
            return
        }

        Prefs.setSuggestedEditsTranslateDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)

        val builder = notificationBuilder(context)

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_big_text)))
                .setContentTitle(context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_title))
                .setContentIntent(pendingIntentBuilder(context, AddTitleDescriptionsActivity::class.java, Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC, 1))
                .addAction(notificationActionBuilder(context, AddTitleDescriptionsActivity::class.java,
                        Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC,
                        R.string.suggested_edits_unlock_notification_button, 1))

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, builder.build())
    }
}
