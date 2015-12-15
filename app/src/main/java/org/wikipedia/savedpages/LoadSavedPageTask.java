package org.wikipedia.savedpages;

import org.wikipedia.page.PageTitle;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

public class LoadSavedPageTask extends SaneAsyncTask<Page> {
    private final PageTitle title;
    private final int sequence;

    public LoadSavedPageTask(PageTitle title) {
        this(title, 0);
    }

    public LoadSavedPageTask(PageTitle title, int sequence) {
        this.title = title;
        this.sequence = sequence;
    }

    @Override
    public Page performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readFromFileSystem();
    }

    public int getSequence() {
        return sequence;
    }
}
