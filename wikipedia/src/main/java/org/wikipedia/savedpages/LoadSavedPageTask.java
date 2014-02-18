package org.wikipedia.savedpages;

import android.content.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;
import org.wikipedia.page.*;

public class LoadSavedPageTask extends SaneAsyncTask<Page> {
    private final WikipediaApp app;
    private final PageTitle title;

    public LoadSavedPageTask(Context context, PageTitle title) {
        super(ExecutorService.getSingleton().getExecutor(LoadSavedPageTask.class, 1));
        app = (WikipediaApp) context.getApplicationContext();
        this.title = title;
    }

    @Override
    public Page performTask() throws Throwable {
        SavedPagePerister persister = (SavedPagePerister) app.getPersister(SavedPage.class);
        return persister.loadPageContent(title);
    }
}
