package org.wikipedia.savedpages;

import android.content.Context;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

public class DeleteSavedPageTask extends SaneAsyncTask<Boolean> {
    private final WikipediaApp app;
    private final SavedPage savedPage;
    public DeleteSavedPageTask(Context context, SavedPage savedPage) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.savedPage = savedPage;
    }

    @Override
    public Boolean performTask() throws Throwable {
        savedPage.deleteFromFileSystem();
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);
        persister.delete(savedPage);
        return true;
    }
}
