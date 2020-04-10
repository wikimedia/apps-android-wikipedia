package org.wikipedia.suggestededits

import android.content.Context
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.settings.Prefs
import java.util.*
import java.util.concurrent.TimeUnit

class SuggestedEditsLocalNotificationTask(context: Context) : RecurringTask() {

    override fun shouldRun(lastRun: Date): Boolean {
        val days = System.currentTimeMillis() - Prefs.getLastDesccriptionEditTime()
        return (days in FIRST_NOTIFICATION_SHOW_ON_DAY until SECOND_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsLocalNotificationShown())
                || (days >= SECOND_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsLocalNotificationShown())
    }

    override fun run(lastRun: Date) {
        val days = System.currentTimeMillis() - Prefs.getLastDesccriptionEditTime()
        if (days in FIRST_NOTIFICATION_SHOW_ON_DAY until SECOND_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsLocalNotificationShown()) {
            Prefs.setSuggestedEditsLocalNotificationShown(true)
            // TODO: show local notification - 3 days after last SE edit
        } else if (days >= SECOND_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsLocalNotificationShown()) {
            Prefs.setSuggestedEditsLocalNotificationShown(false)
            // TODO: show local notification - 7 days after last SE edit
        }
    }

    override fun getName(): String {
        return "suggested-edits-local-notification-task"
    }

    companion object {
        private val FIRST_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(3)
        private val SECOND_NOTIFICATION_SHOW_ON_DAY = TimeUnit.DAYS.toMillis(7)
    }
}