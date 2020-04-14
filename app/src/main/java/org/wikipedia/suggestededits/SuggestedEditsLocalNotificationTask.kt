package org.wikipedia.suggestededits

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.wikipedia.Constants.INTENT_EXTRA_GO_TO_SE_TAB
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.settings.Prefs
import java.util.*
import java.util.concurrent.TimeUnit


class SuggestedEditsLocalNotificationTask(val context: Context) : RecurringTask() {

    override fun shouldRun(lastRun: Date): Boolean {
        val days = System.currentTimeMillis() - Prefs.getLastDescriptionEditTime()
        return (days in FIRST_NOTIFICATION_SHOW_ON_DAY until SECOND_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsLocalNotificationShown())
                || (days >= SECOND_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsLocalNotificationShown())
    }

    override fun run(lastRun: Date) {
        val days = System.currentTimeMillis() - Prefs.getLastDescriptionEditTime()
        if (days in FIRST_NOTIFICATION_SHOW_ON_DAY until SECOND_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsLocalNotificationShown()) {
            Prefs.setSuggestedEditsLocalNotificationShown(true)
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_local_notification_first, false)
        } else if (days >= SECOND_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsLocalNotificationShown()) {
            Prefs.setSuggestedEditsLocalNotificationShown(false)
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_local_notification_second, false)
        }
    }

    override fun getName(): String {
        return "suggested-edits-local-notification-task"
    }

    companion object {
        @JvmStatic
        fun showSuggestedEditsLocalNotification(context: Context, description: Int, forced: Boolean) {
            if (!WikipediaApp.getInstance().isAnyActivityResumed || forced) {
                val intent: Intent = MainActivity.newIntent(context).putExtra(INTENT_EXTRA_GO_TO_SE_TAB, true)
                val builder: NotificationCompat.Builder = NotificationPresenter.getDefaultBuilder(context)
                val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_local_notification_title),
                        context.getString(description), context.getString(description), R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent)
            }
        }
        private val FIRST_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(3)
        private val SECOND_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(7)
    }
}