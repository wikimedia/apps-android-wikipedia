package org.wikipedia.savedpages;

import android.content.Context;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

public class SavePageTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;
    private final PageTitle title;
    private final Page page;

    public SavePageTask(Context context, PageTitle title, Page page) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.title = title;
        this.page = page;
    }

    @Override
    public Void performTask() throws Throwable {
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);

        SavedPage savedPage = new SavedPage(title);
        savedPage.writeToFileSystem(page);

        persister.upsert(savedPage);
        return null;
    }
}
