package org.wikipedia.savedpages;

import android.content.Context;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

public class LoadSavedPageTask extends SaneAsyncTask<Page> {
    private final PageTitle title;
    private final WikipediaApp app;

    public LoadSavedPageTask(Context context, PageTitle title) {
        super(SINGLE_THREAD);
        this.title = title;
        this.app = (WikipediaApp)context.getApplicationContext();
    }

    @Override
    public Page performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readFromFileSystem();
    }
}
