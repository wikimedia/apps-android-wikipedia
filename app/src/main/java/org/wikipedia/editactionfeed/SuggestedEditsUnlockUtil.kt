package org.wikipedia.editactionfeed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.views.DialogTitleWithImage


object SuggestedEditsUnlockUtil {

    private val NOTIFICATION_CHANNEL_ID = "SUGGESTED_EDITS_CHANNEL"
    private val NOTIFICATION_ICON = R.drawable.ic_mode_edit_white_24dp
    private val NOTIFICATION_ICON_COLOR = R.color.accent50

    fun showUnlockAddDescriptionDialog(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (Prefs.isActionEditDescriptionsUnlocked() || Prefs.getTotalUserDescriptionsEdited() < Constants.ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD
                || !ReleaseUtil.isPreBetaRelease()) {
            return
        }
        Prefs.setActionEditDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)
        AlertDialog.Builder(context)
                .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_add_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                .setMessage(R.string.suggested_edits_unlock_add_descriptions_dialog_message)
                .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, Constants.InvokeSource.EDIT_FEED_TITLE_DESC)) }
                .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                .show()
    }

    fun showUnlockTranslateDescriptionDialog(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (WikipediaApp.getInstance().language().appLanguageCodes.size < Constants.MULTILUNGUAL_LANGUAGES_COUNT_MINIMUM || Prefs.getTotalUserDescriptionsEdited() <= Constants.ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD || !Prefs.showEditActionTranslateDescriptionsUnlockedDialog()) {
            return
        }
        Prefs.setActionEditDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)
        AlertDialog.Builder(context)
                .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_translate_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                .setMessage(R.string.suggested_edits_unlock_translate_descriptions_dialog_message)
                .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC)) }
                .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                .show()
    }

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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
    }

    private fun notificationActionBuilder(context: Context,
                              targetClass: Class<*>,
                              intentExtra: String,
                              @DrawableRes buttonDrawable: Int,
                              @StringRes buttonText: Int,
                              requestCode: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) buttonDrawable else android.R.color.transparent,
                context.getString(buttonText),
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

    fun showUnlockAddDescriptionNotification() {
        // TODO: implement this
    }
}
