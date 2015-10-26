package org.wikipedia.page.snippet;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ShareUtil;

import java.io.File;
import java.util.Date;

/**
 * Mainly to clean up images shared through SnippetShareAdapter.
 */
public class SharedImageCleanupTask extends RecurringTask {

    private static final long RUN_INTERVAL_MILLI = 24L * 60L * 60L * 1000L;
    @NonNull private final Context context;

    public SharedImageCleanupTask(Context context) {
        this.context = context;
    }

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @Override
    protected void run(Date lastRun) {
        FileUtil.clearDirectory(new File(ShareUtil.getShareFolder(context), "share"));
    }

    @Override
    protected String getName() {
        return "shared-image-cleanup";
    }
}
