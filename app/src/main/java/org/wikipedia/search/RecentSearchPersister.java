package org.wikipedia.search;

import android.content.Context;

import org.wikipedia.data.ContentPersister;

public class RecentSearchPersister extends ContentPersister<RecentSearch> {
    public RecentSearchPersister(Context context) {
        super(context, RecentSearch.PERSISTENCE_HELPER, RecentSearch.PERSISTENCE_HELPER.getTableName());
    }
}