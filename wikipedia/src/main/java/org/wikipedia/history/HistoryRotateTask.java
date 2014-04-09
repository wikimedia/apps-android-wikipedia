package org.wikipedia.history;

import android.content.*;
import org.wikipedia.*;
import org.wikipedia.recurring.*;

import java.util.*;

public class HistoryRotateTask extends RecurringTask {
    // The 'l' suffix is needed because stupid Java overflows constants otherwise
    private static final long RUN_INTERVAL_MILLI =  24L * 60L * 60L * 1000L;
    private static final long CLEAR_INTERVAL_MILLI = 30L * 24L * 60L * 60L * 1000L;

    public HistoryRotateTask(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        WikipediaApp app = (WikipediaApp) context.getApplicationContext();
        // Clear all entries before this time
        String timeUntil = String.valueOf(System.currentTimeMillis() - CLEAR_INTERVAL_MILLI);
        app.getPersister(HistoryEntry.class).deleteWhere(
                "timestamp < ?",
                new String[] {timeUntil}
        );
    }

    @Override
    protected String getName() {
        return "old-history-clearer";
    }
}
