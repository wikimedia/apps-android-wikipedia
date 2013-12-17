package org.wikimedia.wikipedia.savedpages;

import android.content.Context;
import org.wikimedia.wikipedia.Page;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

import java.util.TimerTask;

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
