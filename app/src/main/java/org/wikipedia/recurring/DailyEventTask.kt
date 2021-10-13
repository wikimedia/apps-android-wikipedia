package org.wikipedia.recurring

import android.content.Context
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.DailyStatsFunnel
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class DailyEventTask(context: Context) : RecurringTask() {
    override val name = context.getString(R.string.preference_key_daily_event_time_task_name)

    override fun shouldRun(lastRun: Date): Boolean {
        return isDailyEventDue(lastRun)
    }

    override fun run(lastRun: Date) {
        onDailyEvent()
    }

    private fun onDailyEvent() {
        logDailyEventReport()
    }

    private fun logDailyEventReport() {
        DailyStatsFunnel(WikipediaApp.getInstance()).log(WikipediaApp.getInstance())
        DailyStatsEvent.log(WikipediaApp.getInstance())
    }

    private fun isDailyEventDue(lastRun: Date): Boolean {
        return timeSinceLastDailyEvent(lastRun) > TimeUnit.DAYS.toMillis(1)
    }

    private fun timeSinceLastDailyEvent(lastRun: Date): Long {
        return min(Int.MAX_VALUE.toLong(), max(0, absoluteTime - lastRun.time))
    }
}
