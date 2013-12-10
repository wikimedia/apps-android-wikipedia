package org.wikimedia.wikipedia.pageimages;

import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.data.DBOpenHelper;
import org.wikimedia.wikipedia.data.SQLiteContentProvider;
import org.wikimedia.wikipedia.history.HistoryEntry;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.persistanceHelper);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
