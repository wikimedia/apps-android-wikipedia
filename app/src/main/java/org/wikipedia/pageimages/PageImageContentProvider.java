package org.wikipedia.pageimages;

import org.wikipedia.WikipediaApp;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.data.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
