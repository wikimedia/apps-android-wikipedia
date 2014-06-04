package org.wikipedia.bookmarks;

import android.content.Context;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

public class DeleteBookmarkTask extends SaneAsyncTask<Boolean> {
    private final WikipediaApp app;
    private final Bookmark bookmark;
    public DeleteBookmarkTask(Context context, Bookmark bookmark) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.bookmark = bookmark;
    }

    @Override
    public Boolean performTask() throws Throwable {
        BookmarkPersister persister = (BookmarkPersister) app.getPersister(Bookmark.class);
        persister.delete(bookmark);
        return true;
    }
}
