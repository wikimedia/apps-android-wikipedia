package org.wikipedia.games.onthisday

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R

object OnThisDayGameDialogs {

    fun showTurnOffNotificationDialog(
        activity: Activity,
        keepThemOnButtonOnClick: (() -> Unit)? = null,
        turnOffButtonOnclick: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Icon)
            .setTitle(R.string.on_this_day_game_turn_off_notification_dialog_title)
            .setMessage(R.string.on_this_day_game_turn_off_notification_dialog_subtitle)
            .setIcon(R.drawable.outline_notifications_off_24)
            .setPositiveButton(R.string.on_this_day_game_turn_off_notification_dialog_positive_btn_label) { _, _ -> keepThemOnButtonOnClick?.invoke() }
            .setNegativeButton(R.string.on_this_day_game_turn_off_notification_dialog_negative_btn_label) { _, _ -> turnOffButtonOnclick?.invoke() }
            .show()
    }

    fun showTurnOnNotificationDialog(
        activity: Activity,
        turnThemOnButtonOnClick: (() -> Unit)? = null,
        keepThemOffButtonOnclick: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Icon)
            .setTitle(R.string.on_this_day_game_turn_on_notification_dialog_title)
            .setMessage(R.string.on_this_day_game_turn_on_notification_dialog_subtitle)
            .setIcon(R.drawable.outline_notifications_active_24)
            .setPositiveButton(R.string.on_this_day_game_turn_on_notification_dialog_positive_btn_label) { _, _ -> turnThemOnButtonOnClick?.invoke() }
            .setNegativeButton(R.string.on_this_day_game_turn_on_notification_dialog_negative_btn_label) { _, _ -> keepThemOffButtonOnclick?.invoke() }
            .show()
    }
}
