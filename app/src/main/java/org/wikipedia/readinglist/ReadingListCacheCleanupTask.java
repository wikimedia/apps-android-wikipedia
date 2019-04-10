package org.wikipedia.readinglist;

import org.wikipedia.recurring.RecurringTask;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Mainly to clean up images shared through SnippetShareAdapter.
 */
public class ReadingListCacheCleanupTask extends RecurringTask {
    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1);

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
    }

    @Override
    protected String getName() {
        return "reading-list-cache-cleanup";
    }
}
