package org.wikipedia.page.shareafact;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ShareUtil;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Mainly to clean up images shared through SnippetShareAdapter.
 */
public class SharedImageCleanupTask extends RecurringTask {

    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(1);
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
        FileUtil.deleteRecursively(new File(ShareUtil.getShareFolder(context), "share"));
    }

    @Override
    protected String getName() {
        return "shared-image-cleanup";
    }
}
