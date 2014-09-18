package org.wikipedia.beta.recurring;

import android.content.Context;
import org.wikipedia.beta.RemoteConfigRefreshTask;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.alphaupdater.AlphaUpdateChecker;
import org.wikipedia.beta.bridge.StyleFetcherTask;
import org.wikipedia.beta.concurrency.ExecutorService;
import org.wikipedia.beta.concurrency.SaneAsyncTask;

import java.util.concurrent.Executor;

public class RecurringTasksExecutor {
    private final Context context;

    public RecurringTasksExecutor(Context context) {
        this.context = context;
    }

    public void run() {
        Executor executor = ExecutorService.getSingleton().getExecutor(RecurringTasksExecutor.class, 1);
        SaneAsyncTask<Void> task = new SaneAsyncTask<Void>(executor) {
            @Override
            public Void performTask() throws Throwable {
                RecurringTask[] allTasks = new RecurringTask[] {
                        // Has list of all rotating tasks that need to be run
                        new RemoteConfigRefreshTask(context),
                        new StyleFetcherTask(context),
                };
                for (RecurringTask task: allTasks) {
                    task.runIfNecessary();
                }
                if (WikipediaApp.getInstance().getReleaseType() == WikipediaApp.RELEASE_ALPHA) {
                    new AlphaUpdateChecker(context).runIfNecessary();
                }
                return null;
            }
        };
        task.execute();
    }
}
