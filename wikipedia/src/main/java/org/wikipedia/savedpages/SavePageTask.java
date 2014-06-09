package org.wikipedia.savedpages;

import android.content.Context;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

public class SavePageTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;
    private final PageTitle title;

    public SavePageTask(Context context, PageTitle title) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.title = title;
    }

    @Override
    public Void performTask() throws Throwable {
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);

        SavedPage savedPage = new SavedPage(title);

        persister.upsert(savedPage);
        return null;
    }
}
