package org.wikipedia.search;

import org.wikipedia.database.SQLiteContentProvider;

public class RecentSearchContentProvider extends SQLiteContentProvider<RecentSearch> {
    public RecentSearchContentProvider() {
        super(RecentSearch.DATABASE_TABLE);
    }
}
