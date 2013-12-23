package org.wikimedia.wikipedia.savedpages;

import android.content.Context;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

import java.util.concurrent.Executor;

public class DeleteSavedPageTask extends SaneAsyncTask<Boolean> {
    private final WikipediaApp app;
    private final SavedPage savedPage;
    public DeleteSavedPageTask(Context context, SavedPage savedPage) {
        super(ExecutorService.getSingleton().getExecutor(DeleteSavedPageTask.class, 1));
        app = (WikipediaApp) context.getApplicationContext();
        this.savedPage = savedPage;
    }

    @Override
    public Boolean performTask() throws Throwable {
        SavedPagePerister persister = (SavedPagePerister) app.getPersister(SavedPage.class);
        persister.delete(savedPage);
        persister.deletePageContent(savedPage.getTitle());
        return true;
    }
}
