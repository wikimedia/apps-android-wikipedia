package org.wikipedia.recurring

import android.content.Context
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import java.util.Date
import java.util.concurrent.TimeUnit

class DailyEventTask(context: Context) : RecurringTask() {
    override val name = context.getString(R.string.preference_key_daily_event_time_task_name)

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) > TimeUnit.DAYS.toMillis(1)
    }

    override fun run(lastRun: Date) {
        DailyStatsEvent.log(WikipediaApp.instance)
        org.wikipedia.analytics.metricsplatform.DailyStatsEvent().log(WikipediaApp.instance)
        EventPlatformClient.refreshStreamConfigs()
    }
}
