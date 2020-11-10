package org.wikipedia.recurring;

import org.wikipedia.WikipediaApp;
import org.wikipedia.alphaupdater.AlphaUpdateChecker;
import org.wikipedia.settings.RemoteConfigRefreshTask;
import org.wikipedia.util.ReleaseUtil;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RecurringTasksExecutor {
    private final WikipediaApp app;

    public RecurringTasksExecutor(WikipediaApp app) {
        this.app = app;
    }

    public void run() {
        Completable.fromAction(() -> {
            RecurringTask[] allTasks = new RecurringTask[] {
                    // Has list of all rotating tasks that need to be run
                    new RemoteConfigRefreshTask(),
                    new DailyEventTask(app)
            };
            for (RecurringTask task: allTasks) {
                task.runIfNecessary();
            }
            if (ReleaseUtil.isAlphaRelease()) {
                new AlphaUpdateChecker(app).runIfNecessary();
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }
}
