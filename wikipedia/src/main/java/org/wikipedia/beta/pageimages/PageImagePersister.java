package org.wikipedia.beta.pageimages;

import android.content.Context;
import org.wikipedia.beta.data.ContentPersister;

public class PageImagePersister extends ContentPersister<PageImage> {
    public PageImagePersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        PageImage.PERSISTANCE_HELPER.getBaseContentURI()
                ),
                PageImage.PERSISTANCE_HELPER
        );
    }
}
