package org.wikipedia.search;

import org.wikipedia.data.SQLiteContentProvider;

public class RecentSearchContentProvider extends SQLiteContentProvider<RecentSearch> {
    public RecentSearchContentProvider() {
        super(RecentSearch.PERSISTENCE_HELPER);
    }
}
