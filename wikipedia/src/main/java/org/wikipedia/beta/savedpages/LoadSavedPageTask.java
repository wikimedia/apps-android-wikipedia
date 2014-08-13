package org.wikipedia.beta.savedpages;

import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.concurrency.SaneAsyncTask;
import org.wikipedia.beta.page.Page;

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
