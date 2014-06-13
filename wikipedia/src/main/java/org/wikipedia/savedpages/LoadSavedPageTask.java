package org.wikipedia.savedpages;

import org.wikipedia.PageTitle;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

public class LoadSavedPageTask extends SaneAsyncTask<Page> {
    private final PageTitle title;

    public LoadSavedPageTask(PageTitle title) {
        super(SINGLE_THREAD);
        this.title = title;
    }

    @Override
    public Page performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readFromFileSystem();
    }
}
