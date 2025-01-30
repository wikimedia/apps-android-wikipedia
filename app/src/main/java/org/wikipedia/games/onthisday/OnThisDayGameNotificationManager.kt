package org.wikipedia.games.onthisday

import android.app.Activity
import org.wikipedia.R
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
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
        NotificationPollBroadcastReceiver.cancelDailyGameNotification(activity)
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
        (activity as? OnThisDayGameActivity)?.scheduleGameNotification()
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
}
