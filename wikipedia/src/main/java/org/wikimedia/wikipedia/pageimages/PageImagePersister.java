package org.wikimedia.wikipedia.pageimages;

import android.content.Context;
import org.wikimedia.wikipedia.data.ContentPersister;
import org.wikimedia.wikipedia.data.SQLiteContentProvider;
import org.wikimedia.wikipedia.history.HistoryEntry;

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
