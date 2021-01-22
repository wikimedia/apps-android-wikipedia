package org.wikipedia.recurring

import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.*

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
        val lastExecutionLog = "$name. Last execution was $lastRunDate."
        if (shouldRun(lastRunDate)) {
            L.d("Executing recurring task, $lastExecutionLog")
            run(lastRunDate)
            Prefs.setLastRunTime(name, absoluteTime)
        } else {
            L.d("Skipping recurring task, $lastExecutionLog")
        }
    }

    protected abstract fun shouldRun(lastRun: Date): Boolean
    protected abstract fun run(lastRun: Date)

    protected abstract val name: String

    protected val absoluteTime: Long
        get() = System.currentTimeMillis()
    private val lastRunDate: Date
        get() = Date(Prefs.getLastRunTime(name))
}
