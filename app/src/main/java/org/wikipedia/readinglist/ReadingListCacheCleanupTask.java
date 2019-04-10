package org.wikipedia.readinglist;

import android.annotation.SuppressLint;

import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.recurring.RecurringTask;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Mainly to clean up junk offline data when saving/removing articles from reading lists.
 */
public class ReadingListCacheCleanupTask extends RecurringTask {
    private static final long RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(30);

    @Override
    protected boolean shouldRun(Date lastRun) {
        return System.currentTimeMillis() - lastRun.getTime() >= RUN_INTERVAL_MILLI;
    }

    @SuppressLint("CheckResult")
    @Override
    protected void run(Date lastRun) {
        Observable.fromCallable(() -> ReadingListDbHelper.instance().getAllPages())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pages -> new SavedPageSyncService().cleanUpJunkFiles(pages), L::d);
    }

    @Override
    protected String getName() {
        return "reading-list-cache-cleanup";
    }


}
