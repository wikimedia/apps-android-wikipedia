package org.wikipedia.pageimages;

import org.wikipedia.database.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.DATABASE_TABLE);
    }
}
