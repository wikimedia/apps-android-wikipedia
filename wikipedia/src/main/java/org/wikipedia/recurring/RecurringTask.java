package org.wikipedia.recurring;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
    protected final Context context;

    protected RecurringTask(Context context) {
        this.context = context;
    }

    protected abstract boolean shouldRun(Date lastRun);

    protected abstract void run(Date lastRun);

    protected abstract String getName();

    public void runIfNecessary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String prefKey = getName() + "-lastrun";
        Date lastRunDate =  new Date(prefs.getLong(prefKey, 0));

        if (shouldRun(lastRunDate)) {
            Log.d("Wikipedia", "Running task " + getName());
            run(lastRunDate);
            prefs.edit().putLong(prefKey, System.currentTimeMillis()).commit();
        } else {
            Log.d("Wikipedia", "Skipping task " + getName());
        }

    }
}
