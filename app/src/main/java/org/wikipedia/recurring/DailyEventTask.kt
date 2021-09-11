package org.wikipedia.recurring

import android.content.Context
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.DailyStatsFunnel

class DailyEventTask(context: Context) : RecurringTask() {
    override val name = context.getString(R.string.preference_key_daily_event_time_task_name)

    override fun run() {
        DailyStatsFunnel(WikipediaApp.getInstance()).log(WikipediaApp.getInstance())
    }
}
