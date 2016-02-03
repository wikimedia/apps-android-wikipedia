package org.wikipedia.savedpages;

import android.content.Context;

import org.wikipedia.data.ContentPersister;

public class SavedPagePersister extends ContentPersister<SavedPage> {
    public SavedPagePersister(Context context) {
        super(context, SavedPage.PERSISTENCE_HELPER, SavedPage.PERSISTENCE_HELPER.getBaseContentURI());
    }
}