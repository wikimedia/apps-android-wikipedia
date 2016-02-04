package org.wikipedia.pageimages;

import org.wikipedia.database.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider {
    public PageImageContentProvider() {
        super(PageImage.DATABASE_TABLE);
    }
}
