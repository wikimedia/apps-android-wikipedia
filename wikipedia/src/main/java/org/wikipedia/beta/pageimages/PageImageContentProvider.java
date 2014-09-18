package org.wikipedia.beta.pageimages;

import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.data.DBOpenHelper;
import org.wikipedia.beta.data.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.PERSISTANCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
