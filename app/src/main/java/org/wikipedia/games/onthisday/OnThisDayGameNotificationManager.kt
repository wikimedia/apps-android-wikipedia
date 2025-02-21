package org.wikipedia.games.onthisday

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.notifications.NotificationPollBroadcastReceiver.Companion.ACTION_DAILY_GAME
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil

enum class OnThisDayGameNotificationState {
    NO_INTERACTED,
    ENABLED,
    DISABLED;

    fun getIcon(): Int = when (this) {
        NO_INTERACTED -> R.drawable.outline_notification_add_24
        ENABLED -> R.drawable.outline_notifications_active_24
        DISABLED -> R.drawable.outline_notifications_off_24
    }
}

object OnThisDayGameNotificationManager {

    private const val NOTIFICATION_TYPE_LOCAL = "local"

    fun handleNotificationClick(activity: Activity) {
        when (Prefs.otdNotificationState) {
            OnThisDayGameNotificationState.ENABLED -> showDisabledNotificationDialog(activity)
            OnThisDayGameNotificationState.NO_INTERACTED,
            OnThisDayGameNotificationState.DISABLED -> showEnabledNotificationDialog(activity)
        }
    }

    private fun showDisabledNotificationDialog(activity: Activity) {
        WikiGamesEvent.submit("impression", "notification_modal", "game_end")
        MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Icon)
            .setTitle(R.string.on_this_day_game_turn_off_notification_dialog_title)
            .setMessage(R.string.on_this_day_game_turn_off_notification_dialog_subtitle)
            .setIcon(R.drawable.outline_notifications_off_24)
            .setPositiveButton(R.string.on_this_day_game_turn_off_notification_dialog_positive_btn_label) { _, _ ->
                WikiGamesEvent.submit("on_click", "notification_modal", "game_end")
                Prefs.otdNotificationState = OnThisDayGameNotificationState.ENABLED
                activity.invalidateOptionsMenu()
            }
            .setNegativeButton(R.string.on_this_day_game_turn_off_notification_dialog_negative_btn_label) { _, _ ->
                WikiGamesEvent.submit("off_click", "notification_modal", "game_end")
                disableNotifications(activity, showUndo = true)
            }
            .show()
    }

    private fun showEnabledNotificationDialog(activity: Activity) {
        WikiGamesEvent.submit("impression", "notification_modal", "game_end")
        MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Icon)
            .setTitle(R.string.on_this_day_game_turn_on_notification_dialog_title)
            .setMessage(R.string.on_this_day_game_turn_on_notification_dialog_subtitle)
            .setIcon(R.drawable.outline_notifications_active_24)
            .setPositiveButton(R.string.on_this_day_game_turn_on_notification_dialog_positive_btn_label) { _, _ ->
                WikiGamesEvent.submit("on_click", "notification_modal", "game_end")
                enableNotifications(activity, showUndo = true)
            }
            .setNegativeButton(R.string.on_this_day_game_turn_on_notification_dialog_negative_btn_label) { _, _ ->
                WikiGamesEvent.submit("off_click", "notification_modal", "game_end")
                Prefs.otdNotificationState = OnThisDayGameNotificationState.DISABLED
                activity.invalidateOptionsMenu()
            }
            .show()
    }

    private fun disableNotifications(activity: Activity, showUndo: Boolean) {
        Prefs.otdNotificationState = OnThisDayGameNotificationState.DISABLED
        cancelDailyGameNotification(activity)
        if (showUndo) {
            FeedbackUtil.makeSnackbar(
                activity,
                activity.getString(R.string.on_this_day_game_notification_turned_off_snackbar_message)
            ).apply {
                setAction(R.string.reading_list_item_delete_undo) {
                    WikiGamesEvent.submit("undo_click", "notification_snackbar", "game_end")
                    enableNotifications(activity, showUndo = false)
                    activity.invalidateOptionsMenu()
                }
            }.show()
        }
        activity.invalidateOptionsMenu()
    }

    private fun enableNotifications(activity: Activity, showUndo: Boolean) {
        Prefs.otdNotificationState = OnThisDayGameNotificationState.ENABLED
        (activity as? OnThisDayGameActivity)?.requestPermissionAndScheduleGameNotification()
        if (showUndo) {
            FeedbackUtil.makeSnackbar(
                activity,
                activity.getString(R.string.on_this_day_game_notification_turned_on_snackbar_message)
            ).apply {
                setAction(R.string.reading_list_item_delete_undo) {
                    WikiGamesEvent.submit("undo_click", "notification_snackbar", "game_end")
                    disableNotifications(activity, showUndo = false)
                    activity.invalidateOptionsMenu()
                }
            }.show()
        }
        activity.invalidateOptionsMenu()
    }

    fun showNotification(context: Context) {
        if (WikipediaApp.instance.currentResumedActivity !is OnThisDayGameActivity &&
            OnThisDayGameViewModel.LANG_CODES_SUPPORTED.contains(WikipediaApp.instance.wikiSite.languageCode)) {
            NotificationPresenter.showNotification(
                context = context,
                builder = NotificationPresenter.getDefaultBuilder(context, 1, NOTIFICATION_TYPE_LOCAL),
                id = 1,
                title = context.getString(R.string.on_this_day_game_feed_entry_card_heading),
                text = context.getString(R.string.on_this_day_game_notification_text),
                longText = context.getString(R.string.on_this_day_game_notification_text),
                lang = null,
                icon = null,
                color = R.color.blue600,
                bodyIntent = OnThisDayGameActivity.newIntent(
                    context = context,
                    invokeSource = Constants.InvokeSource.NOTIFICATION,
                    wikiSite = WikipediaApp.instance.wikiSite
                )
            )
        }
    }

    fun scheduleDailyGameNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(ACTION_DAILY_GAME)
        val timeUntilNextDay = OnThisDayGameFinalFragment.timeUntilNextDay().toMillis()
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + timeUntilNextDay,
            AlarmManager.INTERVAL_DAY,
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        )
    }

    fun cancelDailyGameNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(ACTION_DAILY_GAME)
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
    }
}
