package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.wikipedia.Constants.INTENT_EXTRA_GO_TO_SE_TAB
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.settings.Prefs
import java.util.concurrent.TimeUnit


class SuggestedEditsLocalNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val days = System.currentTimeMillis() - Prefs.getLastDescriptionEditTime()
        if (days in FIRST_NOTIFICATION_SHOW_ON_DAY until SECOND_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsReactivationPassStageOne()) {
            Prefs.setSuggestedEditsReactivationPassStageOne(true)
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_first, false)
        } else if (days >= SECOND_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsReactivationPassStageOne() && Prefs.getLastDescriptionEditTime() > 0) {
            Prefs.setSuggestedEditsReactivationPassStageOne(false)
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_second, false)
        }
        return Result.success()
    }

    companion object {
        @JvmStatic
        fun showSuggestedEditsLocalNotification(context: Context, description: Int, forced: Boolean) {
            if (!WikipediaApp.getInstance().isAnyActivityResumed || forced) {
                val intent: Intent = MainActivity.newIntent(context).putExtra(INTENT_EXTRA_GO_TO_SE_TAB, true)
                val builder: NotificationCompat.Builder = NotificationPresenter.getDefaultBuilder(context)
                NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_reactivation_notification_title),
                        context.getString(description), context.getString(description), R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent)
            }
        }

        private val FIRST_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(3)
        private val SECOND_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(7)
    }
}