package org.wikipedia.recurring

import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Represents a task that needs to be run periodically.
 *
 * Usually an expensive task, that is run Async. Do not do anything
 * that requires access to the UI thread on these tasks.
 *
 * Since it is an expensive task, there's a separate method that detects
 * if the task should be run or not, and then runs it if necessary. The
 * last run times are tracked automatically by the base class.
 */
abstract class RecurringTask {
    fun runIfNecessary() {
        val lastRunDate = lastRunDate
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val lastExecutionLog = "$name. Last execution was ${formatter.format(lastRunDate)}."
        if (shouldRun(lastRunDate)) {
            L.d("Executing recurring task, $lastExecutionLog")
            run()
            Prefs.setLastRunTime(name, System.currentTimeMillis())
        } else {
            L.d("Skipping recurring task, $lastExecutionLog")
        }
    }

    private fun shouldRun(lastRun: LocalDateTime): Boolean {
        return ChronoUnit.DAYS.between(lastRun, LocalDateTime.now()) > 1
    }

    protected abstract fun run()

    protected abstract val name: String

    private val lastRunDate: LocalDateTime
        get() = Instant.ofEpochMilli(Prefs.getLastRunTime(name)).atZone(ZoneId.systemDefault()).toLocalDateTime()
}
