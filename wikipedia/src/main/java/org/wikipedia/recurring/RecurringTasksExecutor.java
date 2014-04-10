package org.wikipedia.recurring;

import android.content.*;
import org.wikipedia.concurrency.ExecutorService;
import org.wikipedia.concurrency.*;
import org.wikipedia.history.*;

import java.util.concurrent.*;

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
                RecurringTask[] tasks = new RecurringTask[] {
                        // Has list of all rotating tasks that need to be run
                        new HistoryRotateTask(context)
                };
                for(RecurringTask task: tasks) {
                    task.runIfNecessary();
                }
                return null;
            }
        };
        task.execute();
    }
}
