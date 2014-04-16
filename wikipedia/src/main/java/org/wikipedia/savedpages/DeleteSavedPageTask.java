package org.wikipedia.savedpages;

import android.content.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

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
        SavedPagePerister persister = (SavedPagePerister) app.getPersister(SavedPage.class);
        persister.delete(savedPage);
        persister.deletePageContent(savedPage.getTitle());
        return true;
    }
}
