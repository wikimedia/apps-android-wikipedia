package org.wikipedia.pageimages;

import android.content.*;
import org.wikipedia.data.*;

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
