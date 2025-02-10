package org.wikipedia.games.onthisday

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
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

class OnThisDayGameNotificationManager(private val activity: Activity) {

    fun handleNotificationClick() {
        when (Prefs.otdNotificationState) {
            OnThisDayGameNotificationState.ENABLED -> showDisabledNotificationDialog()
            OnThisDayGameNotificationState.NO_INTERACTED,
            OnThisDayGameNotificationState.DISABLED -> showEnabledNotificationDialog()
        }
    }

    private fun showDisabledNotificationDialog() {
        OnThisDayGameDialogs.showTurnOffNotificationDialog(
            activity = activity,
            turnOffButtonOnclick = {
                disableNotifications(showUndo = true)
            },
            keepThemOnButtonOnClick = {
                Prefs.otdNotificationState = OnThisDayGameNotificationState.ENABLED
                activity.invalidateOptionsMenu()
            }
        )
    }

    private fun showEnabledNotificationDialog() {
        OnThisDayGameDialogs.showTurnOnNotificationDialog(
            activity = activity,
            turnThemOnButtonOnClick = {
                enableNotifications(showUndo = true)
            },
            keepThemOffButtonOnclick = {
                Prefs.otdNotificationState = OnThisDayGameNotificationState.DISABLED
                activity.invalidateOptionsMenu()
            }
        )
    }

    private fun disableNotifications(showUndo: Boolean) {
        Prefs.otdNotificationState = OnThisDayGameNotificationState.DISABLED
        cancelDailyGameNotification(activity)
        if (showUndo) {
            FeedbackUtil.makeSnackbar(
                activity,
                activity.getString(R.string.on_this_day_game_notification_turned_off_snackbar_message)
            ).apply {
                setAction(R.string.reading_list_item_delete_undo) {
                    enableNotifications(showUndo = false)
                    activity.invalidateOptionsMenu()
                }
            }.show()
        }
        activity.invalidateOptionsMenu()
    }

    private fun enableNotifications(showUndo: Boolean) {
        Prefs.otdNotificationState = OnThisDayGameNotificationState.ENABLED
        (activity as? OnThisDayGameActivity)?.requestPermissionAndScheduleGameNotification()
        if (showUndo) {
            FeedbackUtil.makeSnackbar(
                activity,
                activity.getString(R.string.on_this_day_game_notification_turned_on_snackbar_message)
            ).apply {
                setAction(R.string.reading_list_item_delete_undo) {
                    disableNotifications(showUndo = false)
                    activity.invalidateOptionsMenu()
                }
            }.show()
        }
        activity.invalidateOptionsMenu()
    }

    companion object {
        private const val NOTIFICATION_TYPE_LOCAL = "local"

        fun showNotification(context: Context) {
            if ((WikipediaApp.instance.getResumedActivity() is OnThisDayGameActivity).not()) {
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
                    bodyIntent = MainActivity.newIntent(context).apply {
                        putExtra(Constants.INTENT_GO_TO_WIKI_GAME, true)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
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
}
