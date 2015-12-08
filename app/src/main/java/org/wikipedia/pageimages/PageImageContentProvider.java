package org.wikipedia.pageimages;

import org.wikipedia.data.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.PERSISTENCE_HELPER);
    }
}
