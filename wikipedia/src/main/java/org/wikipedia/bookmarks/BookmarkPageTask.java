package org.wikipedia.bookmarks;

import android.content.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

public class BookmarkPageTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;
    private final PageTitle title;

    public BookmarkPageTask(Context context, PageTitle title) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.title = title;
    }

    @Override
    public Void performTask() throws Throwable {
        BookmarkPersister persister = (BookmarkPersister) app.getPersister(Bookmark.class);

        Bookmark bookmark = new Bookmark(title);

        persister.upsert(bookmark);
        return null;
    }
}
