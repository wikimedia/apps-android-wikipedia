package org.wikipedia.recurring;

import android.util.Log;

import org.wikipedia.settings.Prefs;

import java.util.Date;

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
public abstract class RecurringTask {
    public void runIfNecessary() {
        Date lastRunDate = getLastRunDate();
        String lastExecutionLog = getName() + ". Last execution was " + lastRunDate + ".";

        if (shouldRun(lastRunDate)) {
            Log.d(getClass().getName(), "Executing recurring task, " + lastExecutionLog);
            run(lastRunDate);
            Prefs.setLastRunTime(getName(), getAbsoluteTime());
        } else {
            Log.d(getClass().getName(), "Skipping recurring task, " + lastExecutionLog);
        }
    }

    protected abstract boolean shouldRun(Date lastRun);

    protected abstract void run(Date lastRun);

    protected abstract String getName();

    protected long getAbsoluteTime() {
        return System.currentTimeMillis();
    }

    private Date getLastRunDate() {
        return new Date(Prefs.getLastRunTime(getName()));
    }
}
