package org.wikimedia.wikipedia.savedpages;

import android.content.Context;
import org.wikimedia.wikipedia.data.ContentPersister;

public class SavedPagePerister extends ContentPersister<SavedPage> {
    public SavedPagePerister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SavedPage.persistanceHelper.getBaseContentURI()
                ),
                SavedPage.persistanceHelper
        );
    }
}
