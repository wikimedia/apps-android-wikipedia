package org.wikipedia.pageimages;

import android.content.Context;

import org.wikipedia.data.ContentPersister;

public class PageImagePersister extends ContentPersister<PageImage> {
    public PageImagePersister(Context context) {
        super(context, PageImage.PERSISTENCE_HELPER, PageImage.PERSISTENCE_HELPER.getBaseContentURI());
    }
}