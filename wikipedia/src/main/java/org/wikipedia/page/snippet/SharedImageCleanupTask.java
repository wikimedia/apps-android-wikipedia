package org.wikipedia.page.snippet;

import android.content.Context;

import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.util.ShareUtils;

import java.util.Date;

/**
 * Mainly to clean up images shared through SnippetShareAdapter.
 */
public class SharedImageCleanupTask extends RecurringTask {

    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L;

    public SharedImageCleanupTask(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        ShareUtils.clearFolder(getContext());
    }

    @Override
    protected String getName() {
        return "shared-image-cleanup";
    }
}
