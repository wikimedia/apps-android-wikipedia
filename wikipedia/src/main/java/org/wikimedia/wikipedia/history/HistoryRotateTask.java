package org.wikimedia.wikipedia.history;

import android.content.Context;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.recurring.RecurringTask;

import java.util.Date;

public class HistoryRotateTask extends RecurringTask {
    // Clear things older than 30 days
    private static final long CLEAR_INTERVAL_MILLI =  30 * 24 * 60 * 60 * 1000;

    public HistoryRotateTask(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= CLEAR_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        WikipediaApp app = (WikipediaApp) context.getApplicationContext();
        // Clear all entries before this time
        String timeUntil = String.valueOf(System.currentTimeMillis() - CLEAR_INTERVAL_MILLI);
        app.getPersister(HistoryEntry.class).deleteWhere(
                "timestamp < ?",
                new String[] { timeUntil }
        );
    }

    @Override
    protected String getName() {
        return "old-history-clearer";
    }
}
