package org.wikipedia.pageimages;

import android.content.Context;
import org.wikipedia.data.ContentPersister;

public class PageImagePersister extends ContentPersister<PageImage> {
    public PageImagePersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        PageImage.persistanceHelper.getBaseContentURI()
                ),
                PageImage.persistanceHelper
        );
    }
}
