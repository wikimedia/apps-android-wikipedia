package org.wikipedia.recurring;

import org.wikipedia.RemoteConfigRefreshTask;
import org.wikipedia.WikipediaApp;
import org.wikipedia.alphaupdater.AlphaUpdateChecker;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.snippet.SharedImageCleanupTask;

public class RecurringTasksExecutor {
    private final WikipediaApp app;

    public RecurringTasksExecutor(WikipediaApp app) {
        this.app = app;
    }

    public void run() {
        SaneAsyncTask<Void> task = new SaneAsyncTask<Void>() {
            @Override
            public Void performTask() throws Throwable {
                RecurringTask[] allTasks = new RecurringTask[] {
                        // Has list of all rotating tasks that need to be run
                        new RemoteConfigRefreshTask(app),
                        new SharedImageCleanupTask(app),
                        new DailyEventTask(app)
                };
                for (RecurringTask task: allTasks) {
                    task.runIfNecessary();
                }
                if (WikipediaApp.getInstance().isAlphaRelease()) {
                    new AlphaUpdateChecker(app).runIfNecessary();
                }
                return null;
            }
        };
        task.execute();
    }
}
