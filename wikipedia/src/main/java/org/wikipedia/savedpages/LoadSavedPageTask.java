package org.wikipedia.savedpages;

import android.content.Context;
import org.wikipedia.Page;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;
import org.wikipedia.concurrency.SaneAsyncTask;

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
